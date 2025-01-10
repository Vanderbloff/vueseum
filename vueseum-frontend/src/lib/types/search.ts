// src/lib/types/search.ts

export interface ArtworkSearchCriteria {
	title?: string;
	artistName?: string;
	medium?: string;
	period?: string;
	culture?: string;
	geographicLocation?: string;
	department?: string;
	artworkType?: string;
	isOnDisplay?: boolean;
	museums?: string[];
	tags?: string[];
}