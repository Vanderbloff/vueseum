// src/lib/api/base.ts
import { API_BASE_URL } from '../config';

export class ApiError extends Error {
	constructor(
		public status: number,
		message: string
	) {
		super(message);
		this.name = 'ApiError';
	}
}

export class BaseApiClient {
	protected async fetchWithError<T>(
		endpoint: string,
		options: RequestInit = {}
	): Promise<T> {
		try {
			const url = endpoint.startsWith('http')
				? endpoint
				: new URL(endpoint, API_BASE_URL).toString();

			const response = await fetch(url, {
				...options,
				headers: {
					'Content-Type': 'application/json',
					...options.headers,
				},
			});

			if (!response.ok) {
				throw new ApiError(response.status, await response.text());
			}

			return await response.json();
		} catch (error) {
			// Handle fetch errors (network issues)
			if (error instanceof TypeError && error.message === 'Failed to fetch') {
				throw new ApiError(
					0,
					'Unable to connect to the server. Please try again.'
				);
			}

			// Re-throw API errors as is
			if (error instanceof ApiError) {
				throw error;
			}

			// Log and re-throw other errors
			console.error(`API Error: ${endpoint}`, error);
			throw error;
		}
	}
}