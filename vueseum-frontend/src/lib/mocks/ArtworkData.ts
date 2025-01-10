// src/lib/mocks/ArtworkData.ts
import type { Artwork, PaginatedResponse, StandardPeriod } from '$lib/types/artwork';

// Our collection of mock artworks
export const mockArtworks: Artwork[] = [
	{
		id: 1,
		title: 'The Starry Night',
		artist: 'Vincent van Gogh',
		imageUrl: '/api/placeholder/400/300',
		creationDate: '1889',
		isOnDisplay: true,
		galleryNumber: 'G1',
		medium: '',
		geographicLocation: '',
		fullAttribution: 'Vincent van Gogh',
		isConfidentAttribution: true,
		externalId: '',
		culture: '',
		description: null,
		museum: null,
		tags: []
	},
	{
		id: 2,
		title: 'Girl with a Pearl Earring',
		artist: 'Johannes Vermeer',
		imageUrl: '/api/placeholder/400/300',
		creationDate: '1665',
		isOnDisplay: false,
		galleryNumber: 'G2',
		medium: '',
		geographicLocation: '',
		fullAttribution: 'Johannes Vermeer',
		isConfidentAttribution: true,
		externalId: '',
		culture: '',
		description: null,
		museum: null,
		tags: []
	},
	{
		id: 3,
		title: 'The Persistence of Memory',
		artist: 'Salvador Dalí',
		imageUrl: '/api/placeholder/400/300',
		creationDate: '1931',
		isOnDisplay: true,
		galleryNumber: 'G3',
		medium: '',
		geographicLocation: '',
		fullAttribution: 'Salvador Dalí',
		isConfidentAttribution: true,
		externalId: '',
		culture: '',
		description: null,
		museum: null,
		tags: []
	}
];

interface ArtworkFilters {
	searchTerm: string[];
	searchField: 'all' | 'title' | 'artist' | 'culture';
	objectType: string[];
	culturalRegion: string[];
	era: StandardPeriod[];
	department: string[];
	onDisplay: boolean;
	hasImage: boolean;
}

function sortArtworks(artworks: Artwork[], field: string, direction: 'asc' | 'desc') {
	return [...artworks].sort((a, b) => {
		let comparison = 0;

		switch (field) {
			case 'title':
				comparison = a.title.localeCompare(b.title);
				break;
			case 'artist':
				comparison = (a.artist || '').localeCompare(b.artist || '');
				break;
			case 'date':
				// Convert dates to numbers for comparison, defaulting to 0 if invalid
				{ const dateA = a.creationDate ? new Date(a.creationDate).getTime() : 0;
				const dateB = b.creationDate ? new Date(b.creationDate).getTime() : 0;
				comparison = dateA - dateB;
				break; }
			default: // 'relevance' - maintain current order
				return 0;
		}

		return direction === 'asc' ? comparison : -comparison;
	});
}

function parseYear(yearStr: string): number {
	// Handle various year formats
	if (!yearStr) return 0;

	// Extract first number from string (e.g., "ca. 1885" -> 1885)
	const match = yearStr.match(/\d+/);
	return match ? parseInt(match[0]) : 0;
}

function isYearInPeriod(year: number, periodStr: StandardPeriod): boolean {
	// Handle B.C. periods
	if (periodStr.includes('B.C.')) {
		// Parse numbers from the period (e.g., "2000-1000 B.C." -> [2000, 1000])
		const numbers = periodStr.split('-')
			.map(p => parseInt(p.replace(/[^0-9]/g, '')));

		// Convert to negative years for BC dates and check range
		return year <= -numbers[1] && year >= -numbers[0];
	}

	// Handle A.D. periods
	const yearRange = periodStr
		.replace('A.D. ', '')
		.split('-')
		.map(p => p === 'present' ? new Date().getFullYear() : parseInt(p));

	return year >= yearRange[0] && year <= yearRange[1];
}

export function getMockPaginatedArtworks(
	page: number = 0,
	filters: ArtworkFilters,
	size: number = 10,
	sort?: { field: string; direction: 'asc' | 'desc' }
): PaginatedResponse<Artwork> {
	let filteredArtworks = [...mockArtworks];

	if (hasActiveFilters(filters)) {
		filteredArtworks = filteredArtworks.filter(artwork => {
			// Search term filter
			if (filters.searchTerm.length > 0) {
				const matchesTerm = filters.searchTerm.some(term => {
					const searchTerm = term.toLowerCase();
					switch (filters.searchField) {
						case 'title':
							return artwork.title.toLowerCase().includes(searchTerm);
						case 'artist':
							return artwork.artist.toLowerCase().includes(searchTerm);
						case 'all':
							return artwork.title.toLowerCase().includes(searchTerm) ||
								artwork.artist.toLowerCase().includes(searchTerm) ||
								artwork.medium.toLowerCase().includes(searchTerm);
					}
				});
				if (!matchesTerm) return false;
			}

			// Era filter
			if (filters.era.length > 0) {
				const year = parseYear(artwork.creationDate);
				const matchesEra = filters.era.some(period =>
					isYearInPeriod(year, period)
				);
				if (!matchesEra) return false;
			}

			// Other existing filters
			if (filters.onDisplay && !artwork.isOnDisplay) return false;
			if (filters.hasImage && !artwork.imageUrl) return false;
			if (filters.objectType.length > 0 && !filters.objectType.includes(artwork.medium)) return false;
			if (filters.culturalRegion.length > 0 && !filters.culturalRegion.includes(artwork.geographicLocation)) return false;

			return true;
		});
	}

	// Apply sorting if specified
	if (sort && sort.field !== 'relevance') {
		filteredArtworks = sortArtworks(filteredArtworks, sort.field, sort.direction);
	}

	// Apply pagination
	const start = page * size;
	const content = filteredArtworks.slice(start, start + size);

	return {
		content,
		totalElements: filteredArtworks.length,
		totalPages: Math.ceil(filteredArtworks.length / size),
		size,
		number: page
	};
}

// Helper function to check if any filters are actively set
function hasActiveFilters(filters: ArtworkFilters): boolean {
	return filters.searchTerm.length > 0 ||
		filters.objectType.length > 0 ||
		filters.culturalRegion.length > 0 ||
		filters.era.length > 0 ||
		filters.department.length > 0 ||
		filters.onDisplay ||
		filters.hasImage;
}

// Helper function to find a specific artwork by ID
export function getMockArtworkById(id: number): Artwork | undefined {
	return mockArtworks.find(artwork => artwork.id === id);
}