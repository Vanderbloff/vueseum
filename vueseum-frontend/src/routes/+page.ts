import { getMockPaginatedTours } from '$lib/mocks/TourData';
import type { Load } from '@sveltejs/kit';

// Helper function to simulate API call with delay
async function mockApiCall<T>(getData: () => T, delay: number): Promise<T> {
	await new Promise(resolve => setTimeout(resolve, delay));
	return getData();
}

export const load: Load = async ({ url }) => {
	const artworkPage = Number(url.searchParams.get('artworkPage')) || 0;
	const tourPage = Number(url.searchParams.get('tourPage')) || 0;
	const initialTab = url.searchParams.get('tab') || 'artworks';

	// Only load tours data initially
	const artworks = await mockApiCall(() => getMockPaginatedTours(artworkPage), 1500)
	const tours = await mockApiCall(() => getMockPaginatedTours(tourPage), 1000);

	return {
		artworks,
		tours,
		initialTab
	};
};


/*
export const load: Load = async ({ url }) => {
	// Get page parameters for both tours and artworks
	// Each tab can maintain its own pagination state
	const tourPage = Number(url.searchParams.get('tourPage')) || 0;
	const artworkPage = Number(url.searchParams.get('artworkPage')) || 0;
	const initialTab = url.searchParams.get('tab') || 'artworks';


	// Simulate API delay (in real app, these would be parallel API calls)
	await new Promise(resolve => setTimeout(resolve, 1000));

	return {
		tours: getMockPaginatedTours(tourPage),
		artworks: getMockPaginatedArtworks(artworkPage),
		initialTab: initialTab
	};
};*/
