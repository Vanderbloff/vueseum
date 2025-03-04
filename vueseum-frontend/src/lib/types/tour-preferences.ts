export type TourTheme = "CHRONOLOGICAL" | "ARTIST FOCUSED" | "CULTURAL";

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

export type SuggestionType = 'ARTIST' | 'MEDIUM' | 'CULTURE' | 'PERIOD';

export interface Suggestion {
	value: string;
	display: string;
	count: number;
	type: SuggestionType;
}

export interface TourPreferences {
	theme: TourTheme;
	numStops: number;
	preferredArtworks: string[];
	preferredArtists: string[];
	preferredMediums: string[];
	preferredCultures: string[];
	preferredPeriods: string[];
	preferCloseGalleries: boolean;

	museumId?: number;
	requestId?: string;
}

export interface TourInputState {
	isOpen: boolean;
	selectedMuseum: string;
	tourPreferences: TourPreferences;
	showAdditionalOptions: boolean;
	generatedToursToday: number;
	error: { type: 'DAILY_LIMIT' | 'TOTAL_LIMIT' | null; message: string; } | null;
	isGenerating: boolean;
	generationStage: 'selecting' | 'describing' | 'finalizing' | 'complete' | null;
	descriptionProgress: number,
	currentStopIndex: number | undefined,
	totalStops: number | undefined
}