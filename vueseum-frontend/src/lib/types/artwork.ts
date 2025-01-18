// src/lib/types/artwork.ts
export type StandardPeriod =
	| "2000-1000 B.C."
	| "1000 B.C.-A.D. 1"
	| "A.D. 1-500"
	| "A.D. 500-1000"
	| "A.D. 1000-1400"
	| "A.D. 1400-1600"
	| "A.D. 1600-1800"
	| "A.D. 1800-1900"
	| "A.D. 1900-present";

export interface Museum {
	id: number;
	name: string;
}

export interface Artwork {
	id: number;
	externalId: string;
	title: string;
	artist: string;
	artistPrefix?: string;
	artistRole?: string;
	fullAttribution: string;
	isConfidentAttribution: boolean;
	medium?: string;
	classification?: string;
	culture?: string;
	imageUrl?: string;
	description?: string | null;
	country?: string;
	region?: string;
	galleryNumber?: string | null;
	department?: string;
	isOnDisplay: boolean;
	creationDate?: string;
	museum: Museum | null;
	tags: string[];
}
export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}