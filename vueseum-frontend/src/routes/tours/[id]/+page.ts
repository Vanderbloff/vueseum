// src/routes/tours/[id]/+page.ts
import { mockTour } from '$lib/mocks/TourData';
import type { Load } from '@sveltejs/kit';

export const load: Load = async ({ params }) => {
	await new Promise((resolve) => setTimeout(resolve, 1000));
	return {
		tourId: params.id,
		tour: mockTour,
		loadError: null as string | null
	};
};