// src/lib/mocks/ArtworkData.ts
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';

// Our collection of mock artworks
export const mockArtworks: Artwork[] = [
	{
		id: 1,
		title: 'The Starry Night',
		artist: 'Vincent van Gogh',
		imageUrl: '/api/placeholder/400/300',
		year: '1889',
		isOnDisplay: true,
		galleryNumber: 'G1',
		department: '',
		medium: '',
		culturalRegion: ''
	},
	{
		id: 2,
		title: 'Girl with a Pearl Earring',
		artist: 'Johannes Vermeer',
		imageUrl: '/api/placeholder/400/300',
		year: '1665',
		isOnDisplay: false,
		galleryNumber: 'G2',
		department: '',
		medium: '',
		culturalRegion: ''
	},
	{
		id: 3,
		title: 'The Persistence of Memory',
		artist: 'Salvador Dalí',
		imageUrl: '/api/placeholder/400/300',
		year: '1931',
		isOnDisplay: true,
		galleryNumber: 'G3',
		department: '',
		medium: '',
		culturalRegion: ''
	}
];

// Add the filter interface
interface ArtworkFilters {
	searchTerm: string[];
	searchField: 'all' | 'title' | 'artist' | 'medium';
	objectType: string[];
	culturalRegion: string[];
	era: string[];
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
				{ const dateA = a.year ? new Date(a.year).getTime() : 0;
				const dateB = b.year ? new Date(b.year).getTime() : 0;
				comparison = dateA - dateB;
				break; }
			default: // 'relevance' - maintain current order
				return 0;
		}

		return direction === 'asc' ? comparison : -comparison;
	});
}

// Function to get paginated artwork data, matching the tour data pattern
export function getMockPaginatedArtworks(
	page: number = 0,
	filters: ArtworkFilters,
	size: number = 10,
	sort?: { field: string; direction: 'asc' | 'desc' }
): PaginatedResponse<Artwork> {
	// First, apply filters
	let filteredArtworks = [...mockArtworks];

	if (hasActiveFilters(filters)) {
		filteredArtworks = filteredArtworks.filter(artwork => {
			if (filters.searchTerm.length > 0) {
				const matchesTerm = filters.searchTerm.some(term => {
					const searchTerm = term.toLowerCase();
					switch (filters.searchField) {
						case 'title':
							return artwork.title.toLowerCase().includes(searchTerm);
						case 'artist':
							return artwork.artist.toLowerCase().includes(searchTerm);
						case 'medium':
							return artwork.medium.toLowerCase().includes(searchTerm);
						case 'all':
							return artwork.title.toLowerCase().includes(searchTerm) ||
								artwork.artist.toLowerCase().includes(searchTerm) ||
								artwork.medium.toLowerCase().includes(searchTerm);
					}
				});
				if (!matchesTerm) return false;
			}

			// On Display filter
			if (filters.onDisplay && !artwork.isOnDisplay) {
				return false;
			}

			// Has Image filter
			if (filters.hasImage && !artwork.imageUrl) {
				return false;
			}

			// Object Type filter
			if (filters.objectType.length > 0 && !filters.objectType.includes(artwork.medium)) {
				return false;
			}

			// Location filter
			if (filters.culturalRegion.length > 0 && !filters.culturalRegion.some(loc => artwork.culturalRegion === loc)) {
				return false;
			}

			// Era filter
			if (filters.era.length > 0) {
				const year = parseInt(artwork.year);
				const matchesEra = filters.era.some(era => {
					switch (era.toLowerCase()) {
						case 'ancient':
							return year <= 500;
						case 'medieval':
							return year > 500 && year <= 1500;
						case 'modern':
							return year > 1500;
						default:
							return false;
					}
				});
				if (!matchesEra) return false;
			}

			// Department filter
			return !(filters.department.length > 0 && !filters.department.includes(artwork.department));
		});
	}

	if (sort && sort.field !== 'relevance') {
		filteredArtworks = sortArtworks(filteredArtworks, sort.field, sort.direction);
	}

	// Then apply pagination
	const start = page * size;
	const content = filteredArtworks.slice(start, start + size);

	console.log("getMockPaginatedArtworks called with:", {
		page,
		size,
		filteredLength: filteredArtworks.length,
		returnedContent: content,
		calculatedTotalPages: Math.ceil(filteredArtworks.length / size)
	});

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