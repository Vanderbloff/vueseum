// src/routes/+page.ts
import { artworkApi } from '$lib/api/artwork';
import type { Load } from '@sveltejs/kit';
import { error } from '@sveltejs/kit';
import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';
import { tourApi } from '$lib/api/tour';

export const load: Load = async ({ url }) => {
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

		const toursPromise = tourApi.getTours(tourPage);

		const [artworks, tours] = await Promise.all([
			artworksPromise,
			toursPromise
		]);

		return {
			artworks,
			tours,
			initialTab
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