// src/lib/types/tour.ts
export interface TourStop {
	id: number;
	sequenceNumber: number;
	artwork: {
		id: number;
		title: string;
		artist: string;
		imageUrl: string;
		department: string;
		galleryNumber: string;
		year: string;
	};
	description: string;
	recommendedDuration: number;
	isRequired: boolean;
}

export interface Tour {
	id: number;
	name: string;
	description: string;
	theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
	estimatedDuration: number;
	stops: TourStop[];
	museum: {
		id: number;
		name: string;
		location: string;
	};
}

export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}