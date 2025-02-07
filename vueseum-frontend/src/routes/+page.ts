// src/routes/+page.ts
import type { Load } from '@sveltejs/kit';

export const prerender = true;

export const load: Load = async ({ url }) => {
	// Only return initial state configuration
	return {
		initialTab: url.searchParams.get('tab') || 'artworks',
		initialFilters: {
			searchTerm: [url.searchParams.get('q') ?? ''],
			searchField: (url.searchParams.get('searchField') ?? 'all') as 'all' | 'title' | 'artist' | 'culture',
			objectType: url.searchParams.getAll('objectType'),
			materials: url.searchParams.getAll('medium'),
			country: url.searchParams.getAll('country'),
			region: url.searchParams.getAll('region'),
			culture: url.searchParams.getAll('culture'),
			era: url.searchParams.getAll('period'),
			hasImage: true,
			museumId: url.searchParams.getAll('museumId')
		},
		initialSort: {
			field: (url.searchParams.get('sortBy') ?? 'relevance') as 'relevance' | 'title' | 'artist' | 'date',
			direction: (url.searchParams.get('sortDirection') ?? 'asc') as 'asc' | 'desc'
		}
	};
};