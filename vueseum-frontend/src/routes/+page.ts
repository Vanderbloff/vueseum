// src/routes/+page.ts
import { artworkApi } from '$lib/api/artwork';
import type { Load } from '@sveltejs/kit';
import { error } from '@sveltejs/kit';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';
import { tourApi } from '$lib/api/tour';
import { getOrCreateFingerprint } from '$lib/api/device';
import type { StandardPeriod } from '$lib/types/artwork';
import type { PageData } from './types';

export const load = (async ({ url }): Promise<PageData> => {
	try {
		// Initialize fingerprint early to ensure it's available for all API calls
		await getOrCreateFingerprint();
	} catch (fingerprintError) {
		console.error('Failed to initialize device fingerprint:', fingerprintError);
		// Continue with page load even if fingerprint fails
	}

	const searchParams = url.searchParams;
	const initialFilters = {
		searchTerm: [searchParams.get('q') ?? ''],
		searchField: (searchParams.get('searchField') ?? 'all') as 'all' | 'title' | 'artist' | 'culture',
		category: searchParams.getAll('category'),
		origin: searchParams.getAll('origin'),
		era: searchParams.getAll('period') as StandardPeriod[],
		hasImage: false,
		museumId: searchParams.getAll('museumId')
	}

	const sort = {
		field: (searchParams.get('sortBy') ?? 'relevance') as 'relevance' | 'title' | 'artist' | 'date',
		direction: (searchParams.get('sortDirection') ?? 'asc') as 'asc' | 'desc'
	};

	const artworkPage = Number(url.searchParams.get('artworkPage')) || 0;
	const tourPage = Number(url.searchParams.get('tourPage')) || 0;
	const initialTab = url.searchParams.get('tab') || 'artworks';

	try {
		const filterOptionsPromise = artworkApi.getFilterOptions({});

		const artworksPromise = initialTab === 'artworks'
			? await artworkApi.searchArtworks(
				ArtworkUtils.getDefaultFilters(),
				artworkPage
			)
			: {
				content: [],
				totalElements: 0,
				totalPages: 0,
				size: 20,
				number: 0
			};

		const toursPromise = initialTab === 'tours' || initialTab === null
			? await tourApi.getTours(tourPage)
			: {
				content: [],
				totalElements: 0,
				totalPages: 0,
				size: 10,
				number: 0
			};

		const [artworks, tours, filterOptions] = await Promise.all([
			artworksPromise,
			toursPromise,
			filterOptionsPromise
		]);

		return {
			artworks,
			tours,
			initialTab: url.searchParams.get('tab') || 'artworks',
			initialFilters,
			initialSort: sort,
			filterOptions
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
}) satisfies Load;