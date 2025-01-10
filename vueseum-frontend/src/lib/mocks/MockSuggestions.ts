type SuggestionType = 'ARTWORK' | 'ARTIST' | 'MEDIUM' | 'CULTURE' | 'PERIOD';

export const mockSuggestions = {
	ARTWORK: [
		{ value: "The Starry Night", display: "The Starry Night (1889)", count: 1, type: "ARTWORK" as SuggestionType },
		{ value: "The Persistence of Memory", display: "The Persistence of Memory (1931)", count: 1, type: "ARTWORK" as SuggestionType },
		{ value: "Girl with a Pearl Earring", display: "Girl with a Pearl Earring (1665)", count: 1, type: "ARTWORK" as SuggestionType }
	],
	ARTIST: [
		{ value: "Vincent van Gogh", display: "Vincent van Gogh (1853-1890)", count: 12, type: "ARTIST" as SuggestionType },
		{ value: "Claude Monet", display: "Claude Monet (1840-1926)", count: 15, type: "ARTIST" as SuggestionType },
		{ value: "Pablo Picasso", display: "Pablo Picasso (1881-1973)", count: 20, type: "ARTIST" as SuggestionType }
	],
	MEDIUM: [
		{ value: "Oil on canvas", display: "Oil on canvas", count: 100, type: "MEDIUM" as SuggestionType },
		{ value: "Watercolor", display: "Watercolor", count: 50, type: "MEDIUM" as SuggestionType },
		{ value: "Sculpture", display: "Sculpture", count: 30, type: "MEDIUM" as SuggestionType }
	],
	CULTURE: [
		{ value: "French", display: "French (Western Europe)", count: 200, type: "CULTURE" as SuggestionType },
		{ value: "Italian", display: "Italian (Southern Europe)", count: 180, type: "CULTURE" as SuggestionType },
		{ value: "Japanese", display: "Japanese (East Asia)", count: 150, type: "CULTURE" as SuggestionType }
	]
};

// Optional: Add delay to simulate network latency
function delay(ms: number) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

// Mock API function
export async function mockFetchSuggestions(prefix: string, type: string) {
	await delay(500); // Simulate network delay

	const suggestions = mockSuggestions[type as keyof typeof mockSuggestions] || [];
	return suggestions.filter(s =>
		s.value.toLowerCase().includes(prefix.toLowerCase())
	);
}