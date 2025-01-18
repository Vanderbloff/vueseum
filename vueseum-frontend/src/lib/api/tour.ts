// src/lib/api/tour.ts
import { API_BASE_URL } from '../config';
import { ApiError, BaseApiClient } from '$lib/api/base';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import type { Tour } from '$lib/types/tour';
import { goto } from '$app/navigation';

interface TourPreferences {
	museumId: number;
	theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
	maxStops: number;
	minStops: number;
	preferredArtworks: string[];
	preferredArtists: string[];
	preferredMediums: string[];
	preferredCultures: string[];
	preferredPeriods: string[];
}

export class TourApiClient extends BaseApiClient {
	private readonly baseUrl = `${API_BASE_URL}/tours`;

	async getTours(page: number = 0, size: number = 10): Promise<PaginatedResponse<Tour>> {
		if (import.meta.env.DEV) {
			// Check if we're in a browser environment
			if (typeof window !== 'undefined') {
				// Get tours from localStorage
				const storedTours = JSON.parse(localStorage.getItem('devTours') || '[]');

				const start = page * size;
				const end = start + size;
				const paginatedTours = storedTours.slice(start, end);

				return {
					content: paginatedTours,
					totalElements: storedTours.length,
					totalPages: Math.ceil(storedTours.length / size),
					size: size,
					number: page
				};
			}

			// Return empty response for SSR
			return {
				content: [],
				totalElements: 0,
				totalPages: 0,
				size: size,
				number: page
			};
		}
		return this.fetchWithError<PaginatedResponse<Tour>>(
			`${this.baseUrl}?page=${page}&size=${size}`
		);
	}

	async getTourById(id: number): Promise<Tour> {
		if (import.meta.env.DEV) {
			if (typeof window !== 'undefined') {
				const storedTours = JSON.parse(localStorage.getItem('devTours') || '[]');
				const tour = storedTours.find((t: Tour) => t.id === id);
				if (!tour) {
					throw new Error('Tour not found');
				}
				return tour;
			}
			throw new Error('Tour not found');
		}
		const response = await this.fetchWithError<Tour>(`${this.baseUrl}/${id}`);
		if (!response) {
			throw new Error('Tour not found');
		}
		return response;
	}

	async deleteTour(id: number): Promise<void> {
		if (import.meta.env.DEV) {
			if (typeof window !== 'undefined') {
				const storedTours = JSON.parse(localStorage.getItem('devTours') || '[]');
				const updatedTours = storedTours.filter((t: Tour) => t.id !== id);
				localStorage.setItem('devTours', JSON.stringify(updatedTours));
				return;
			}
			throw new Error('Tour not found');
		}
		await this.fetchWithError(
			`${this.baseUrl}/${id}`,
			{ method: 'DELETE' }
		);
	}
	async updateTour(
		id: number,
		updates: { name?: string; description?: string }
	): Promise<Tour> {
		if (import.meta.env.DEV) {
			if (typeof window !== 'undefined') {
				const storedTours = JSON.parse(localStorage.getItem('devTours') || '[]');
				const tourIndex = storedTours.findIndex((t: Tour) => t.id === id);

				if (tourIndex === -1) {
					throw new Error('Tour not found');
				}

				const updatedTour = {
					...storedTours[tourIndex],
					...updates
				};

				storedTours[tourIndex] = updatedTour;
				localStorage.setItem('devTours', JSON.stringify(storedTours));

				return updatedTour;
			}
			throw new Error('Tour not found');
		}
		return this.fetchWithError<Tour>(
			`${this.baseUrl}/${id}`,
			{
				method: 'PATCH',
				body: JSON.stringify(updates)
			}
		);
	}

	async generateTour(visitorId: string, preferences: TourPreferences): Promise<Tour> {
		if (import.meta.env.DEV) {
			return this.generateDevTour(preferences);
		}

		try {
			return await this.fetchWithError<Tour>(
				`${this.baseUrl}/generate`,
				{
					method: 'POST',
					body: JSON.stringify({
						visitorId,
						preferences: {
							museumId: preferences.museumId,
							theme: preferences.theme,
							maxStops: preferences.maxStops,
							minStops: preferences.minStops,
							requiredArtworkIds: preferences.preferredArtworks,
							preferredArtistIds: preferences.preferredArtists,
							preferredMediums: preferences.preferredMediums,
							preferredCultures: preferences.preferredCultures,
							preferredPeriods: preferences.preferredPeriods
						}
					})
				}
			);
		} catch (error) {
			if (error instanceof ApiError) {
				if (error.status === 507) {
					throw new Error('TOTAL_LIMIT');
				} else if (error.status === 429) {
					throw new Error('DAILY_LIMIT');
				} else if (error.status === 400) {
					throw new Error('INVALID_REQUEST');
				}
			}
			throw error;
		}
	}

	private async generateDevTour(preferences: TourPreferences): Promise<Tour> {
		// Simulate network delay
		await new Promise(resolve => setTimeout(resolve, 1000));

		// Check limits using localStorage
		const totalTours = parseInt(localStorage.getItem('devTotalTours') || '0');
		const dailyTours = parseInt(localStorage.getItem('devDailyTours') || '0');

		if (totalTours >= 10) {
			throw new Error('TOTAL_LIMIT');
		}
		if (dailyTours >= 3) {
			throw new Error('DAILY_LIMIT');
		}

		// Get artworks for the selected museum
		const response = await fetch(`${API_BASE_URL}/artworks`);
		const allArtworks = await response.json();
		const museumArtworks = allArtworks.filter(
            (a: { museum: { id: number; }; }) => a.museum.id === preferences.museumId
		);

		// Select artworks based on preferences
		const selectedArtworks = this.selectArtworksForDevTour(
			museumArtworks,
			preferences
		);

		// Update limits
		localStorage.setItem('devTotalTours', (totalTours + 1).toString());
		localStorage.setItem('devDailyTours', (dailyTours + 1).toString());

		const tourId = parseInt(localStorage.getItem('devTourCounter') || '0') + 1;
		localStorage.setItem('devTourCounter', tourId.toString());

		// Create tour response
		const newTour = {
			id: tourId,
			name: `${preferences.theme} Tour`,
			description: 'A personalized tour based on your preferences.',
			stops: selectedArtworks.map((artwork, index) => ({
				id: index + 1,
				sequenceNumber: index + 1,
				artwork: {
					id: artwork.id,
					title: artwork.title,
					artist: artwork.artist,
					artistPrefix: artwork.artistPrefix,
					artistRole: artwork.artistRole,
					fullAttribution: artwork.fullAttribution,
					medium: artwork.medium,
					classification: artwork.classification,
					country: artwork.country,
					region: artwork.region,
					culture: artwork.culture,
					imageUrl: artwork.imageUrl,
					description: artwork.description,
					galleryNumber: artwork.galleryNumber,
					department: artwork.department,
					creationDate: artwork.creationDate,
					isOnDisplay: artwork.isOnDisplay
				},
				tourContextDescription: `${artwork.description}`,
				isRequired: index < preferences.minStops
			})),
			museum: {
				id: preferences.museumId,
				name: museumArtworks[0]?.museum.name || 'Metropolitan Museum of Art'
			},
			theme: preferences.theme
		};

		if (typeof window !== 'undefined') {
			// Save to localStorage
			const existingTours = JSON.parse(localStorage.getItem('devTours') || '[]');
			localStorage.setItem('devTours', JSON.stringify([...existingTours, newTour]));
		}

		await goto(`/tours/${newTour.id}`);
		return newTour;
	}

	private selectArtworksForDevTour(artworks: Artwork[], preferences: TourPreferences) {
		const selected: Artwork[] = [];

		// First, add preferred artworks if they exist
		if (preferences.preferredArtworks.length > 0) {
			const preferred = artworks.filter(
				a => preferences.preferredArtworks.includes(a.id.toString())
			);
			selected.push(...preferred);
		}

		// Then fill remaining slots randomly
		while (selected.length < preferences.maxStops) {
			const remaining = artworks.filter(a => !selected.includes(a));
			if (remaining.length === 0) break;

			const randomIndex = Math.floor(Math.random() * remaining.length);
			selected.push(remaining[randomIndex]);
		}

		return selected;
	}
}

// Create a singleton instance
export const tourApi = new TourApiClient();