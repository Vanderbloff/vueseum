// src/lib/types/tour.ts
export interface TourStop {
	id: number;
	sequenceNumber: number;
	artwork: {
		id: number;
		title: string;
		artist: string;
		artistPrefix?: string;
		artistRole?: string;
		fullAttribution: string;
		medium?: string;
		classification?: string;
		culture?: string;
		country?: string;
		region?: string;
		imageUrl?: string;
		description?: string | null;
		galleryNumber?: string | null;
		department?: string;
		creationDate?: string;
		isOnDisplay: boolean;
	};
	tourContextDescription: string;
	isRequired: boolean;
}

export interface Tour {
	id: number;
	name: string;
	description: string;
	theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
	stops: TourStop[];
	museum: {
		id: number;
		name: string;
	};
}

export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}