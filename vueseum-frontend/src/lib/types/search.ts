// src/lib/types/search.ts

export interface ArtworkSearchCriteria {
	title?: string;
	artistName?: string;
	medium?: string;
	period?: string;
	artworkType?: string;
	museums?: string[];
	tags?: string[];
	hasImage?: boolean;
	origin?: string;
	category?: string;
}