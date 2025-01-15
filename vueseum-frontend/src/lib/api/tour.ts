// src/lib/api/tour.ts
import { API_BASE_URL } from '../config';
import { ApiError, BaseApiClient } from '$lib/api/base';

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

interface TourGenerationResponse {
	requestId: string;
}

export class TourApiClient extends BaseApiClient {
	private readonly baseUrl = `${API_BASE_URL}/tours`;

	async generateTour(visitorId: string, preferences: TourPreferences) {
		if (import.meta.env.DEV) {
			// Create a new tour in JSON Server
			const newTour = {
				id: Date.now(), // Generate a unique ID
				name: "Generated Tour",
				description: "Auto-generated tour based on preferences",
				theme: preferences.theme,
				museum: {
					id: preferences.museumId,
					name: preferences.museumId === 1 ? "Metropolitan Museum of Art" : "Louvre Museum"
				},
				stops: []
			};

			const response = await this.fetchWithError<TourGenerationResponse>(
				`${this.baseUrl}`,
				{
					method: 'POST',
					body: JSON.stringify(newTour)
				}
			);

			// Return mock generation response
			return {
				requestId: response.toString()
			};
		}

		try {
			return await this.fetchWithError<TourGenerationResponse>(
				`${this.baseUrl}/generate`,
				{
					method: 'POST',
					body: JSON.stringify({ visitorId, preferences })
				}
			);
		} catch (error) {
			if (error instanceof ApiError) {
				if (error.status === 507) {
					throw new Error('TOTAL_LIMIT');
				} else if (error.status === 429) {
					throw new Error('DAILY_LIMIT');
				}
			}
			throw error;
		}
	}

	async getTourProgress(requestId: string): Promise<{
		progress: number;
		currentTask: string;
		hasError: boolean;
		errorMessage?: string;
	}> {
		if (import.meta.env.DEV) {
			// Simulate progress
			return {
				progress: 1, // Always return complete for development
				currentTask: "Tour generated",
				hasError: false
			};
		}

		return this.fetchWithError(
			`${this.baseUrl}/generation/${requestId}/status`
		);
	}

	async monitorTourProgress(
		requestId: string,
		onProgress: (progress: number, task: string) => void
	): Promise<void> {
		const progressTimeout = new ProgressTimeout(() => {
			throw new Error('Tour generation appears to be stuck. No progress updates received.');
		});

		try {
			progressTimeout.start();

			while (true) {
				const status = await this.getTourProgress(requestId);
				progressTimeout.updateProgress(); // Reset timeout on any status update

				onProgress(status.progress, status.currentTask);

				if (status.hasError) {
					throw new Error(status.errorMessage || 'Tour generation failed');
				}

				if (status.progress >= 1) {
					break;
				}

				// Wait before next check
				await new Promise(resolve => setTimeout(resolve, 1000));
			}
		} finally {
			progressTimeout.stop();
		}
	}
}

class ProgressTimeout {
	private lastProgressUpdate: number;
	private timeoutId: number | null = null;

	constructor(
		private readonly onTimeout: () => void,
		private readonly maxStaleTime: number = 30000
	) {
		this.lastProgressUpdate = Date.now();
	}

	updateProgress() {
		this.lastProgressUpdate = Date.now();
	}

	start() {
		this.check();
	}

	private check() {
		const timeSinceUpdate = Date.now() - this.lastProgressUpdate;

		if (timeSinceUpdate > this.maxStaleTime) {
			this.onTimeout();
			return;
		}

		this.timeoutId = window.setTimeout(() => this.check(), 5000);
	}

	stop() {
		if (this.timeoutId !== null) {
			window.clearTimeout(this.timeoutId);
			this.timeoutId = null;
		}
	}
}

// Create a singleton instance
export const tourApi = new TourApiClient();