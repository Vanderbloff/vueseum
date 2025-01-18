// src/lib/api/artwork.ts

import type { ArtworkSearchCriteria } from '$lib/types/search';
import { criteriaToUrlParams, mapFiltersToSearchCriteria } from '$lib/types/filterMapping';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import { API_BASE_URL } from '../config';
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

export class ArtworkApiClient extends BaseApiClient {
	private readonly baseUrl = `${API_BASE_URL}/artworks`;

	async searchArtworks(
		filters: ArtworkFilters,
		page: number = 0,
		size: number = 20,
		sort?: { field: string; direction: 'asc' | 'desc' }
	): Promise<PaginatedResponse<Artwork>> {
		if (import.meta.env.DEV) {
			// Fetch all artworks
			// Apply filters
			let filteredData = await this.fetchWithError<Artwork[]>(
				`${this.baseUrl}`
			);

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

			if (filters.onDisplay) {
				filteredData = filteredData.filter(artwork => artwork.isOnDisplay);
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
		return this.fetchWithError<PaginatedResponse<Artwork>>(
			`${this.baseUrl}?${params}`
		);
	}

	async getArtwork(id: string, museumId: number): Promise<Artwork> {
		if (import.meta.env.DEV) {
			// JSON Server supports direct ID lookup
			return this.fetchWithError<Artwork>(
				`${this.baseUrl}/${id}`
			);
		}
		return this.fetchWithError<Artwork>(
			`${this.baseUrl}/${id}?museumId=${museumId}`
		);
	}

	async getFilterOptions(
		criteria: Partial<ArtworkSearchCriteria>
	): Promise<FilterOptions> {
		if (import.meta.env.DEV) {
			const artworks = await this.fetchWithError<Artwork[]>(`${this.baseUrl}`);

			// Extract unique non-null values
			const classifications = [...new Set(
				artworks
					.map(a => a.classification)
					.filter((c): c is string => c !== undefined)
			)];

			// Extract unique countries and regions
			const countries = [...new Set(
				artworks
					.map(a => a.country)
					.filter((c): c is string =>
						c !== undefined && c !== null && c !== '')
			)];

			// If artwork type is specified, return relevant materials
			if (criteria.artworkType) {
				const relevantMedia = artworks
					.filter(a => a.classification === criteria.artworkType)
					.map(a => a.medium)
					.filter((m): m is string => m !== undefined);

				return {
					objectType: classifications,
					materials: [...new Set(relevantMedia)],
					countries,
					regions: [],
					cultures: []
				};
			}

			// If country is specified, return relevant regions
			if (criteria.country) {
				const relevantRegions = artworks
					.filter(a => a.country === criteria.country)
					.map(a => a.region)
					.filter((r): r is string => r !== undefined);

				// Get cultures for the selected country
				const relevantCultures = artworks
					.filter(a => a.country === criteria.country)
					.map(a => a.culture)
					.filter((c): c is string => c !== undefined);

				return {
					objectType: classifications,
					materials: [],
					countries,
					regions: [...new Set(relevantRegions)],
					cultures: [...new Set(relevantCultures)]
				};
			}

			// If region is specified (and implicitly country is specified)
			if (criteria.region) {
				const relevantCultures = artworks
					.filter(a =>
						a.country === criteria.country &&
						a.region === criteria.region
					)
					.map(a => a.culture)
					.filter((c): c is string => c !== undefined);

				return {
					objectType: classifications,
					materials: [],
					countries,
					regions: [], // Regions already loaded from country selection
					cultures: [...new Set(relevantCultures)]
				};
			}

			// Default: return top-level options
			return {
				objectType: classifications,
				materials: [],
				countries,
				regions: [],
				cultures: []
			};
		}

		// Production endpoint
		const params = new URLSearchParams();
		Object.entries(criteria).forEach(([key, value]) => {
			if (value !== undefined) {
				params.append(key, value.toString());
			}
		});

		return this.fetchWithError<FilterOptions>(
			`${this.baseUrl}/filter-options?${params}`
		);
	}
}

// Create a singleton instance
export const artworkApi = new ArtworkApiClient();