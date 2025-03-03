import { API_BASE_URL } from '../config';
import { ApiError, BaseApiClient } from '$lib/api/base';
import { getOrCreateFingerprint } from './device';
import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
import type { Tour, TourValidationResult } from '$lib/types/tour';

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
	constructor() {
		super('/tours');
	}

	async getTours(page: number = 0, size: number = 10): Promise<PaginatedResponse<Tour>> {
		if (import.meta.env.DEV) {
			if (typeof window !== 'undefined') {
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
			return this.getEmptyPaginatedResponse(size, page);
		}

		// Ensure fingerprint is initialized before making the API call
		await getOrCreateFingerprint();
		return this.fetchWithError<PaginatedResponse<Tour>>(`?page=${page}&size=${size}`);
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

		await getOrCreateFingerprint();
		const tour = await this.fetchWithError<Tour>(`/${id}`);
		if (!tour) {
			throw new Error('Tour not found');
		}
		return tour;
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

		await getOrCreateFingerprint();
		await this.fetchWithError(`/${id}`, { method: 'DELETE' });
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

		await getOrCreateFingerprint();
		return this.fetchWithError<Tour>(
			`/${id}`,
			{
				method: 'PATCH',
				body: JSON.stringify(updates)
			}
		);
	}

	async generateTour(preferences: TourPreferences): Promise<Tour> {
		if (import.meta.env.DEV) {
			return this.generateDevTour(preferences);
		}

		try {
			// Get or initialize the fingerprint for this device
			const visitorId = await getOrCreateFingerprint();

			console.log("Tour generation request:", {
				visitorId,
				preferences
			});

			const response = await this.fetchWithError<Tour>(
				'/generate',
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

			console.log("Tour generation raw response:", response);

			// Check if the response has the expected structure
			if (!response) {
				console.error("Tour generation returned null/undefined response");
				throw new Error("Invalid response from server");
			}

			// Verify the id exists and is valid
			if (response.id === undefined || response.id === null) {
				console.error("Tour ID is missing from response:", response);
				throw new Error("Tour ID is missing from response");
			}

			// Ensure ID is a number before returning
			const tourId = Number(response.id);
			if (isNaN(tourId) || tourId === 0) {
				console.error("Invalid tour ID:", response.id, "Parsed as:", tourId);
				throw new Error("Invalid tour ID received from server");
			}

			const tour: Tour = {
				...response,
				id: tourId
			};

			console.log("Processed tour object:", tour);
			return tour;
		} catch (error) {
			console.error("Tour generation error:", error);
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

	async validateTour(id: number): Promise<TourValidationResult> {
		if (import.meta.env.DEV) {
			if (typeof window !== 'undefined') {
				const storedTours = JSON.parse(localStorage.getItem('devTours') || '[]');
				const tourIndex = storedTours.findIndex((t: Tour) => t.id === id);

				if (tourIndex === -1) {
					throw new Error('Tour not found');
				}

				if (!storedTours[tourIndex].unavailableArtworks) {
					const unavailableArtworks = storedTours[tourIndex].stops
						.filter(() => Math.random() < 0.2)
						.map((stop: { artwork: { id: number; title: string; galleryNumber: string } }) => ({
							id: stop.artwork.id,
							title: stop.artwork.title,
							galleryNumber: stop.artwork.galleryNumber
						}));

					storedTours[tourIndex] = {
						...storedTours[tourIndex],
						unavailableArtworks,
						lastValidated: new Date().toISOString()
					};

					localStorage.setItem('devTours', JSON.stringify(storedTours));
				}

				return {
					tourId: id,
					unavailableArtworks: storedTours[tourIndex].unavailableArtworks,
					validatedAt: storedTours[tourIndex].lastValidated
				};
			}
			throw new Error('Tour not found');
		}

		await getOrCreateFingerprint();
		return this.fetchWithError<TourValidationResult>(`/${id}/validate`);
	}

	protected getEmptyPaginatedResponse<T = Tour>(size: number, page: number): PaginatedResponse<T> {
		return {
			content: [] as T[],
			totalElements: 0,
			totalPages: 0,
			size,
			number: page
		};
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

		const response = await fetch(
			`${API_BASE_URL}/artworks?museum=${preferences.museumId}&size=100`
		);
		const museumArtworks = await response.json();

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
					imageUrl: artwork.primaryImageUrl,
					description: artwork.description,
					galleryNumber: artwork.galleryNumber,
					department: artwork.department,
					creationDate: artwork.creationDate,
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