// src/routes/tours/[id]/+page.ts
import { error, type Load } from '@sveltejs/kit';
import { tourApi } from '$lib/api/tour';
import { getOrCreateFingerprint } from '$lib/api/device';

export const load: Load = async ({ params }) => {
	try {
		await getOrCreateFingerprint();

		const tourIdParam = params.id;
		const tourId = parseInt(<string>params.id);

		if (isNaN(tourId)) {
			throw error(400, {
				message: 'Invalid tour ID format'
			});
		}

		const tour = await tourApi.getTourById(tourId);

		return {
			tourId,
			tourIdParam,
			tour,
			loadError: null as string | null
		};
	} catch (e) {
		console.error('Error loading tour:', e);

		if (e instanceof Error) {
			return {
				tourId: params.id,
				tour: null,
				loadError: e.message
			};
		}

		return {
			tourId: params.id,
			tour: null,
			loadError: 'An unexpected error occurred while loading the tour'
		};
	}
};