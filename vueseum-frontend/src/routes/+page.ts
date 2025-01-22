// src/routes/+page.ts
import { artworkApi } from '$lib/api/artwork';
import type { Load } from '@sveltejs/kit';
import { error } from '@sveltejs/kit';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';
import { tourApi } from '$lib/api/tour';
import type { StandardPeriod } from '$lib/types/artwork';

export const load: Load = async ({ url }) => {
	const searchParams = url.searchParams;
	const initialFilters = {
		searchTerm: [searchParams.get('q') ?? ''],
		searchField: (searchParams.get('searchField') ?? 'all') as 'all' | 'title' | 'artist' | 'culture',
		objectType: searchParams.getAll('objectType'),
		materials: searchParams.getAll('medium'),
		country: searchParams.getAll('country'),
		region: searchParams.getAll('region'),
		culture: searchParams.getAll('culture'),
		era: searchParams.getAll('period') as StandardPeriod[],
		onDisplay: searchParams.get('onDisplay') === 'true',
		hasImage: true
	};

	const sort = {
		field: (searchParams.get('sortBy') ?? 'relevance') as 'relevance' | 'title' | 'artist' | 'date',
		direction: (searchParams.get('sortDirection') ?? 'asc') as 'asc' | 'desc'
	};

	const artworkPage = Number(url.searchParams.get('artworkPage')) || 0;
	const tourPage = Number(url.searchParams.get('tourPage')) || 0;
	const initialTab = url.searchParams.get('tab') || 'artworks';

	try {
		const artworksPromise = initialTab === 'artworks'
			? await artworkApi.searchArtworks(
				ArtworkUtils.getDefaultFilters(),
				artworkPage
			)
			: null;

		const toursPromise = initialTab === 'tours' || initialTab === null
			? await tourApi.getTours(tourPage)
			: null;

		const [artworks, tours] = await Promise.all([
			artworksPromise,
			toursPromise
		]);

		return {
			artworks,
			tours,
			initialTab,
			initialFilters,
			initialSort: sort
		};
	} catch (e) {
		console.error('Error loading initial data:', e);
		if (e instanceof Error) {
			throw error(500, {
				message: 'Failed to load initial data'
			});
		}

		throw error(500, {
			message: 'An unexpected error occurred'
		});
	}
};