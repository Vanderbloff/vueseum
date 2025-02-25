import type { StandardPeriod } from './artwork';

export type SearchField = 'all' | 'title' | 'artist' | 'culture';

export interface ArtworkFilters {
	searchTerm: string[];
	searchField: SearchField;
	objectType: string[];
	materials: string[];
	country: string[];
	region: string[];
	culture: string[];
	era: StandardPeriod[];
	hasImage: boolean;
	museumId: string[];
}

export type FilterChangeHandler = (
	key: string,
	value: string[] | boolean | StandardPeriod[] | SearchField
) => void;

export interface FilterOptionsResponse {
	objectType: string[];
	mediums: string[];
	geographicLocations: string[];
	regions: string[];
	cultures: string[];
}