// src/lib/api/artwork.ts

import type { ArtworkSearchCriteria } from '$lib/types/search';
import { criteriaToUrlParams, mapFiltersToSearchCriteria } from '$lib/types/filterMapping';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import { BaseApiClient } from '$lib/api/base';
import type { ArtworkFilters } from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
import { DateUtils } from '$lib/utils/dateUtils';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';

export interface FilterOptions {
	objectType: string[];      // Top-level categories
	materials: string[];       // Available with type selection
	countries: string[];       // Available countries
	regions: string[];        // Regions for selected country
	cultures: string[];        // Available when region is selected
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
	): Promise<FilterOptions> {
		if (import.meta.env.DEV) {
			// Cache artworks to prevent multiple fetches during initialization
			if (!this.cachedArtworks) {
				this.cachedArtworks = await this.fetchWithError<Artwork[]>('');
			}
			const artworks = this.cachedArtworks;

			// Helper function to get unique non-null values
			const getUniqueValues = <T>(
				items: Array<T | undefined | null>,
				filter?: (item: T) => boolean
			): T[] => {
				return [...new Set(
					items.filter((item): item is T =>
						item !== undefined && item !== null &&
						(!filter || filter(item)))
				)].sort();
			};

			// Base object for response
			const options: FilterOptions = {
				objectType: [],
				materials: [],
				countries: [],
				regions: [],
				cultures: []
			};

			// Always include object types and countries as base options
			options.objectType = getUniqueValues(
				artworks.map((a : Artwork) => a.classification)
			);
			options.countries = getUniqueValues(
				artworks.map((a : Artwork) => a.country),
				country => country.trim().length > 0
			);

			// Filter artworks based on criteria
			let filteredArtworks = artworks;

			// Apply artwork type filter
			if (criteria.artworkType) {
				filteredArtworks = filteredArtworks.filter(
					(a : Artwork) => a.classification === criteria.artworkType
				);
				options.materials = getUniqueValues(
					filteredArtworks.map((a : Artwork) => a.medium)
				);
			}

			// Apply country filter
			if (criteria.country) {
				filteredArtworks = filteredArtworks.filter(
					(a : Artwork) => a.country === criteria.country
				);
				options.regions = getUniqueValues(
					filteredArtworks.map((a : Artwork) => a.region)
				);

				// If no region specified, get all cultures for country
				if (!criteria.region) {
					options.cultures = getUniqueValues(
						filteredArtworks.map((a : Artwork) => a.culture)
					);
				}
			}

			// Apply region filter
			if (criteria.region) {
				filteredArtworks = filteredArtworks.filter(
					(a : Artwork) => a.region === criteria.region
				);
				options.cultures = getUniqueValues(
					filteredArtworks.map((a : Artwork) => a.culture)
				);
			}

			return options;
		}

		// Production endpoint
		const params = new URLSearchParams();
		Object.entries(criteria).forEach(([key, value]) => {
			if (value !== undefined) {
				params.append(key, value.toString());
			}
		});

		return this.fetchWithError<FilterOptions>(`/filter-options?${params}`);
	}
}

// Create a singleton instance
export const artworkApi = new ArtworkApiClient();