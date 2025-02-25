// src/lib/utils/filterMapping.ts


import type { ArtworkSearchCriteria } from './search';
import type { ArtworkFilters } from '$lib/types/filters';

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
					criteria.origin = searchTerm;
					break;
				case 'all':
					criteria.title = searchTerm;
					break;
			}
		}
	}

	if (filters.category.length > 0) {
		criteria.category = filters.category[0].split(' (')[0];
	}

	if (filters.origin.length > 0) {
		criteria.origin = filters.origin[0].split(' (')[0];
	}

	// Map period/era
	if (filters.era.length > 0) {
		criteria.period = filters.era[0];
		console.log(`Setting period filter: ${filters.era[0]}`);
	}

	// Explicitly map hasImage
	criteria.hasImage = filters.hasImage;

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