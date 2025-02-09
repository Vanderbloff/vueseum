// src/routes/tours/[id]/+page.ts
import type { Tour } from '$lib/types/tour';

interface PageData {
	tourId: string;
	tour: Tour | null;
	loadError: string | null;
}

export const load = ({ params }): PageData => {
	return {
		tourId: params.id,
		tour: null,
		loadError: null
	};
};