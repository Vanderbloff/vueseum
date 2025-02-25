// src/lib/api/artwork.ts
import type { ArtworkSearchCriteria } from '$lib/types/search';
import { criteriaToUrlParams, mapFiltersToSearchCriteria } from '$lib/types/filterMapping';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import { BaseApiClient } from '$lib/api/base';
import { DateUtils } from '$lib/utils/dateUtils';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';
import type { ArtworkFilters } from '$lib/types/filters';

export interface FilterOptions {
	objectType: string[];
	materials: string[];
	geographicLocations: string[];
	regions: string[];
	cultures: string[];
}

export class ArtworkApiClient extends BaseApiClient {
	constructor() {
		super('/artworks');
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

			// Apply era filter
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
		const params = criteriaToUrlParams(criteria, page, size, sort);
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
	): Promise<FilterOptions> {
		// Create query params
		const params = new URLSearchParams();
		Object.entries(criteria).forEach(([key, value]) => {
			if (value !== undefined) {
				params.append(key, value.toString());
			}
		});

		// Get response from API
		const response = await this.fetchWithError<FilterOptions>(
			`/filter-options?${params}`
		);

		// Return filtered and sorted data
		return {
			objectType: response.objectType?.filter(Boolean).sort() ?? [],
			materials: response.materials?.filter(Boolean).sort() ?? [],
			geographicLocations: response.geographicLocations?.filter(Boolean).sort() ?? [],
			regions: response.regions?.filter(Boolean).sort() ?? [],
			cultures: response.cultures?.filter(Boolean).sort() ?? []
		};
	}
}

// Create a singleton instance
export const artworkApi = new ArtworkApiClient();