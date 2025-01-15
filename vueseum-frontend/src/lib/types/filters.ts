export interface FilterOptions {
	// Top-level categories
	objectType: string[];    // Available when no type is selected
	subtypes: string[];      // Available when type is selected
	materials: string[];     // Available when type is selected

	// Cultural hierarchy
	culturalRegions: string[]; // Available when no region selected
	cultures: string[];      // Available when region is selected
}

export interface FilterState {
	selectedType?: string;    // Selected top-level type
	selectedSubtype?: string; // Selected subtype (when type is selected)
	materials: string[];      // Can select multiple materials
	selectedRegion?: string;  // Selected cultural region
	selectedCulture?: string; // Selected culture (when region is selected)
}