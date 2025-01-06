// src/routes/tours/[id]/+page.ts
import { mockTour } from '$lib/mocks/TourData';
import type { Load } from '@sveltejs/kit';

export const load: Load = async ({ params }) => {
	// Simulate an API delay
	await new Promise(resolve => setTimeout(resolve, 1000));

	return {
		tourId: params.id,
		// In a real app, we'd fetch this from an API
		// For testing, we'll just return our mock data
		tour: mockTour
	};
};