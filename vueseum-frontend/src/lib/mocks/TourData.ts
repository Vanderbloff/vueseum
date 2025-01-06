// src/lib/mocks/tourData.ts
import type { Tour } from '$lib/types/tour';

export const mockTour: Tour = {
	id: 1,
	name: 'Impressionist Masterpieces',
	description:
		"Journey through the most influential Impressionist artworks, exploring the movement's unique approach to light, color, and everyday subjects. This tour showcases how artists broke from tradition to capture fleeting moments of modern life.",
	theme: 'CHRONOLOGICAL',
	estimatedDuration: 90,
	museum: {
		id: 1,
		name: 'Metropolitan Museum of Art',
		location: 'New York'
	},
	stops: [
		{
			id: 1,
			sequenceNumber: 1,
			artwork: {
				id: 101,
				title: 'The Water Lily Pond',
				artist: 'Claude Monet',
				//imageUrl: "https://www.claude-monet.com/assets/img/paintings/water-lily-pond-with-japanese-bridge.jpg",
				imageUrl: '/api/placeholder/400/300',
				department: '19th and Early 20th Century European Paintings and Sculpture',
				galleryNumber: '819',
				year: ''
			},
			description:
				"One of Monet's most famous works from his water lily series, showcasing his mastery of light and reflection. Notice how the artist captures the interplay of light on the water's surface.",
			recommendedDuration: 15,
			isRequired: true
		},
		{
			id: 2,
			sequenceNumber: 2,
			artwork: {
				id: 102,
				title: 'The Dancing Class',
				artist: 'Edgar Degas',
				imageUrl: '/api/placeholder/400/300',
				//imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Edgar_Degas_-_La_Classe_de_danse.jpg/640px-Edgar_Degas_-_La_Classe_de_danse.jpg',
				department: '19th and Early 20th Century European Paintings and Sculpture',
				galleryNumber: '815',
				year: ''
			},
			description:
				'Degas was fascinated by ballet dancers, capturing both their public performances and private rehearsals. This piece demonstrates his masterful ability to capture movement and light.',
			recommendedDuration: 20,
			isRequired: false
		},
		{
			id: 3,
			sequenceNumber: 3,
			artwork: {
				id: 103,
				title: 'Luncheon of the Boating Party',
				artist: 'Pierre-Auguste Renoir',
				imageUrl: '/api/placeholder/400/300',
				//imageUrl: 'https://www.phillipscollection.org/sites/default/files/styles/feature_extra_large_no_crop_1200_/public/collection/1637.jpg?itok=XH83pQkf0',
				department: '19th and Early 20th Century European Paintings and Sculpture',
				galleryNumber: '821',
				year: ''
			},
			description:
				'This vibrant scene captures the joie de vivre of Parisian social life. Notice how Renoir uses dappled light and loose brushstrokes to create a sense of spontaneity and warmth.',
			recommendedDuration: 25,
			isRequired: true
		}
	]
};

// For the tour list page
export const mockTours: Tour[] = [
	mockTour,
	{
		id: 2,
		name: "Modern Art Exploration",
		description: "Discover the revolutionary artists who shaped modern art...",
		theme: "ARTIST_FOCUSED",
		estimatedDuration: 75,
		museum: {
			id: 1,
			name: "Metropolitan Museum of Art",
			location: "New York"
		},
		stops: [] // Add stops following the same pattern
	},
	{
		id: 3,
		name: "Cultural Crossroads",
		description: "Experience the intersection of Eastern and Western artistic traditions...",
		theme: "CULTURAL",
		estimatedDuration: 120,
		museum: {
			id: 1,
			name: "Metropolitan Museum of Art",
			location: "New York"
		},
		stops: [] // Add stops following the same pattern
	}
];

// Mock API response structure for paginated data
export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}

export function getMockPaginatedTours(page: number = 0, size: number = 10): PaginatedResponse<Tour> {
	const start = page * size;
	const content = mockTours.slice(start, start + size);

	return {
		content,
		totalElements: mockTours.length,
		totalPages: Math.ceil(mockTours.length / size),
		size,
		number: page
	};
}