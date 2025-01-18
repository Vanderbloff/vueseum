// src/lib/utils/filterMapping.ts

import type { ArtworkFilters } from '../components/homepage/artwork/ArtworkFilters.svelte';
import type { ArtworkSearchCriteria } from '../types/search';

export function mapFiltersToSearchCriteria(
	filters: ArtworkFilters
): ArtworkSearchCriteria {
	const criteria: ArtworkSearchCriteria = {};

	// Handle search term based on search field
	if (filters.searchTerm.length > 0) {
		const searchTerm = filters.searchTerm[0]?.trim();
		if (searchTerm) {
			switch (filters.searchField) {
				case 'title':
					criteria.title = searchTerm;
					break;
				case 'artist':
					criteria.artistName = searchTerm;
					break;
				case 'culture':
					criteria.culture = searchTerm;
					break;
				case 'all':
					criteria.title = searchTerm;  // Default to title search for 'all'
					break;
			}
		}
	}

	// Map geographic filters hierarchically
	if (filters.country.length > 0) {
		criteria.country = filters.country[0];

		if (filters.region.length > 0) {
			criteria.region = filters.region[0];
		}
	}

	// Map cultural filter
	if (filters.culture.length > 0) {
		criteria.culture = filters.culture[0];
	}

	// Map artwork type and medium
	if (filters.objectType.length > 0) {
		criteria.artworkType = filters.objectType[0];
	}
	if (filters.materials.length > 0) {
		criteria.medium = filters.materials[0];
	}

	// Map period/era filter
	if (filters.era.length > 0) {
		criteria.period = filters.era[0];
	}

	// Map display status
	if (filters.onDisplay) {
		criteria.isOnDisplay = true;
	}

	return criteria;
}

// Function to convert to URL parameters for API calls
export function criteriaToUrlParams(
	criteria: ArtworkSearchCriteria,
	page: number = 0,
	size: number = 20,
	sort?: { field: string; direction: 'asc' | 'desc' }
): URLSearchParams {
	const params = new URLSearchParams();

	// Add all non-undefined criteria to params
	Object.entries(criteria).forEach(([key, value]) => {
		if (value !== undefined) {
			params.append(key, value.toString());
		}
	});

	if (sort && sort.field !== 'relevance') {
		params.append('sortField', sort.field);
		params.append('sortDirection', sort.direction);
	}

	// Add pagination
	params.append('page', page.toString());
	params.append('size', size.toString());

	return params;
}