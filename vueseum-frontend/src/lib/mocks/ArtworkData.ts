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
		medium: ''
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
		medium: ''
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
		medium: ''
	}
];

// Add the filter interface
interface ArtworkFilters {
	searchTerm: string;
	searchField: 'all' | 'title' | 'artist' | 'medium';
	objectType: string;
	location: string;
	era: string;
	department: string;
	onDisplay: boolean;
	hasImage: boolean;
}

// Function to get paginated artwork data, matching the tour data pattern
export function getMockPaginatedArtworks(
	page: number = 0,
	filters?: ArtworkFilters,
	size: number = 10
): PaginatedResponse<Artwork> {
	// First, apply filters if they exist
	let filteredArtworks = [...mockArtworks];

	if (filters) {
		filteredArtworks = filteredArtworks.filter(artwork => {
			// Search term filter
			if (filters.searchTerm) {
				const searchTerm = filters.searchTerm.toLowerCase();
				switch (filters.searchField) {
					case 'title':
						if (!artwork.title.toLowerCase().includes(searchTerm)) return false;
						break;
					case 'artist':
						if (!artwork.artist.toLowerCase().includes(searchTerm)) return false;
						break;
					case 'all':
						{ const matchesTitle = artwork.title.toLowerCase().includes(searchTerm);
						const matchesArtist = artwork.artist.toLowerCase().includes(searchTerm);
						if (!matchesTitle && !matchesArtist) return false;
						break; }
				}
			}

			// On Display filter
			if (filters.onDisplay && !artwork.isOnDisplay) {
				return false;
			}

			// Has Image filter (all our mock data has images, but in real app this would matter)
			if (filters.hasImage && !artwork.imageUrl) {
				return false;
			}

			// Era filter (simple implementation based on year)
			if (filters.era) {
				const year = parseInt(artwork.year);
				switch (filters.era) {
					case 'ancient':
						if (year > 500) return false;
						break;
					case 'medieval':
						if (year <= 500 || year > 1500) return false;
						break;
					case 'modern':
						if (year <= 1500) return false;
						break;
				}
			}

			return true;
		});
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

// Helper function to find a specific artwork by ID
export function getMockArtworkById(id: number): Artwork | undefined {
	return mockArtworks.find(artwork => artwork.id === id);
}