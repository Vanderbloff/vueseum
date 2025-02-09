// src/routes/+page.ts
import type { ArtworkFilters } from "$lib/components/homepage/artwork/ArtworkFilters.svelte";
import type { PaginatedResponse, Tour } from '$lib/types/tour';
import type { StandardPeriod } from '$lib/types/artwork';

export interface PageData {
	initialTab: string;
	initialFilters: ArtworkFilters;
	initialSort: {
		field: 'relevance' | 'title' | 'artist' | 'date';
		direction: 'asc' | 'desc';
	};
	tours?: PaginatedResponse<Tour>;
}

export const ssr = false;
export const prerender = false;

export const load = () => {
	return {
		initialTab: 'artworks',
		initialFilters: {
			searchTerm: [''],
			searchField: 'all',
			objectType: [],
			materials: [],
			country: [],
			region: [],
			culture: [],
			era: [] as StandardPeriod[],
			hasImage: true,
			museumId: []
		},
		initialSort: {
			field: 'relevance',
			direction: 'asc'
		}
	};
};