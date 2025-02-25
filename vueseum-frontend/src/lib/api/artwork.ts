// src/lib/api/artwork.ts

import type { ArtworkSearchCriteria } from '$lib/types/search';
import { criteriaToUrlParams, mapFiltersToSearchCriteria } from '$lib/types/filterMapping';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import { BaseApiClient } from '$lib/api/base';
import { DateUtils } from '$lib/utils/dateUtils';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';
import type { ArtworkFilters, FilterOptionsResponse } from '$lib/types/filters';

export interface FilterOptions {
	objectType: string[];
	mediums: string[];
	geographicLocations: string[];
	regions: string[];
	cultures: string[];
}

/**
 * Cache artwork data in development mode only.
 * This improves filter performance by preventing repeated fetches.
 * In production, the backend handles filtering with proper database queries.
 *
 * Note: This is specifically for development convenience and would not be
 * an appropriate pattern for production where we rely on backend pagination
 * and filtering.
 */
export class ArtworkApiClient extends BaseApiClient {
	constructor() {
		super('/artworks');  // This sets up the base URL correctly
	}

	private cachedArtworks: Artwork[] | null = null;

	async searchArtworks(
		filters: ArtworkFilters,
		page: number = 0,
		size: number = 20,
		sort?: { field: string; direction: 'asc' | 'desc' }
	): Promise<PaginatedResponse<Artwork>> {
		if (import.meta.env.DEV) {
			// Fetch all artworks
			// Apply filters
			let filteredData = await this.fetchWithError<Artwork[]>('');

			filteredData = ArtworkUtils.filterArtworks(filteredData, filters);

			// Apply sorting if needed
			if (sort && sort.field !== 'relevance') {
				filteredData = ArtworkUtils.sortArtworks(
					filteredData,
					sort.field,
					sort.direction
				);
			}

			if (filters.country?.length > 0) {
				filteredData = filteredData.filter(
					artwork => artwork.country === filters.country[0]
				);

				if (filters.region?.length > 0) {
					filteredData = filteredData.filter(
						artwork => artwork.region === filters.region[0]
					);
				}
			}

			// Apply other filters
			if (filters.objectType?.length > 0) {
				filteredData = filteredData.filter(
					artwork => artwork.classification &&
						filters.objectType.includes(artwork.classification)
				);
			}

			if (filters.materials?.length > 0) {
				filteredData = filteredData.filter(
					artwork => artwork.medium &&
						filters.materials.includes(artwork.medium)
				);
			}

			if (filters.culture?.length > 0) {
				filteredData = filteredData.filter(
					artwork => artwork.culture &&
						filters.culture.includes(artwork.culture)
				);
			}

			if (filters.era?.length > 0) {
				filteredData = filteredData.filter(artwork => {
					if (!artwork.creationDate) return false;
					try {
						const year = DateUtils.extractYear(artwork.creationDate);
						return DateUtils.isYearInPeriod(year, filters.era[0]);
					} catch (error) {
						console.error('Error processing date:', error);
						return false;
					}
				});
			}

			const start = page * size;
			const end = Math.min(start + size, filteredData.length);
			const items = filteredData.slice(start, end);

			return {
				content: items,
				totalElements: filteredData.length,
				totalPages: Math.ceil(filteredData.length / size),
				size,
				number: page
			};
		}

		const criteria = mapFiltersToSearchCriteria(filters);
		const params = criteriaToUrlParams(criteria, page, size);
		return this.fetchWithError<PaginatedResponse<Artwork>>(`?${params}`);
	}

	async getArtwork(id: string, museumId: number): Promise<Artwork> {
		if (import.meta.env.DEV) {
			// JSON Server supports direct ID lookup
			return this.fetchWithError<Artwork>(`/${id}`);
		}
		return this.fetchWithError<Artwork>(`/${id}?museumId=${museumId}`);
	}

	async getFilterOptions(
		criteria: Partial<ArtworkSearchCriteria>
	): Promise<FilterOptionsResponse> {
		// Create query params
		const params = new URLSearchParams();
		Object.entries(criteria).forEach(([key, value]) => {
			if (value !== undefined) {
				params.append(key, value.toString());
			}
		});

		// Get response from API
		const response = await this.fetchWithError<FilterOptionsResponse>(
			`/filter-options?${params}`
		);

		// Return filtered and sorted data
		return {
			objectType: response.objectType?.filter(Boolean).sort() ?? [],
			mediums: response.mediums?.filter(Boolean).sort() ?? [],
			geographicLocations: response.geographicLocations?.filter(Boolean).sort() ?? [],
			regions: response.regions?.filter(Boolean).sort() ?? [],
			cultures: response.cultures?.filter(Boolean).sort() ?? []
		};
	}
}

// Create a singleton instance
export const artworkApi = new ArtworkApiClient();