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
	lastValidated?: string;
	unavailableArtworks?: UnavailableArtworkInfo[];
	createdAt?: string;
}

export interface TourValidationResult {
	tourId: number;
	unavailableArtworks: UnavailableArtworkInfo[];
	validatedAt: string;
}

export interface UnavailableArtworkInfo {
	artworkId: number;
	title: string;
	galleryNumber: string;
}

export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}