// src/lib/types/artwork.ts

// This represents a single artwork piece in our system
export interface Artwork {
	id: number;                 // Unique identifier for the artwork
	title: string;             // Title of the piece
	artist: string;            // Artist's name
	year: string;              // Year created (string to handle "c. 1500" type dates)
	imageUrl: string;          // URL to the artwork image
	isOnDisplay: boolean;      // Whether the artwork is currently on display
	culturalRegion: string;
	department: string;
	medium: string;
	galleryNumber?: string;    // Optional: Specific gallery identifier
}

export interface ArtworkSort {
	field: 'relevance' | 'title' | 'artist' | 'date';
	direction: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	size: number;
	number: number;
}