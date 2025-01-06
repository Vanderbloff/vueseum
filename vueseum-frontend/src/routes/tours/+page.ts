// src/routes/tours/+page.ts
import { getMockPaginatedTours } from '$lib/mocks/TourData';
import type { Load } from '@sveltejs/kit';

export const load: Load = async ({ url }) => {
	// Get page from URL query parameters or default to 0
	const page = Number(url.searchParams.get('page')) || 0;

	// Simulate API delay
	await new Promise(resolve => setTimeout(resolve, 1000));

	return {
		tours: getMockPaginatedTours(page)
	};
};