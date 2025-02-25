import type { PaginatedResponse, Artwork, StandardPeriod } from '$lib/types/artwork';
import type { FilterOptions } from '$lib/api/artwork';
import type { Tour } from '$lib/types/tour';

export interface PageData {
	artworks: PaginatedResponse<Artwork>;
	tours: PaginatedResponse<Tour>;
	initialTab: string;
	initialFilters: {
		searchTerm: string[];
		searchField: 'all' | 'title' | 'artist' | 'culture';
		category: string[];
		origin: string[]
		era: StandardPeriod[];
		hasImage: boolean;
		museumId: string[];
	};
	initialSort: {
		field: string;
		direction: 'asc' | 'desc';
	};
	filterOptions: FilterOptions;
}