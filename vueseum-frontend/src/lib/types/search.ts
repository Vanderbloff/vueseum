// src/lib/types/search.ts

export interface ArtworkSearchCriteria {
	title?: string;
	artistName?: string;
	medium?: string;
	period?: string;
	culture?: string;
	geographicLocation?: string;
	region?: string;
	artworkType?: string;
	museums?: string[];
	tags?: string[];
	hasImage?: boolean;
}