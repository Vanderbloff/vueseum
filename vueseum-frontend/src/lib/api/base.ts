// src/lib/api/base.ts
import { API_BASE_URL } from '$lib/config';

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
	protected readonly basePath: string;

	constructor(endpoint: string) {
		this.basePath = endpoint;
	}

	protected async fetchWithError<T>(
		path: string,
		options: RequestInit = {}
	): Promise<T> {
		const fullUrl = `${API_BASE_URL}/api/v1${this.basePath}${path}`;
		console.log('Attempting to fetch:', fullUrl);

		try {
			const response = await fetch(fullUrl, {
				...options,
				credentials: 'include', // Keep this for CORS/CSRF
				headers: {
					'Content-Type': 'application/json',
					...options.headers,
				},
			});

			if (!response.ok) {
				console.log('Response not OK:', {
					status: response.status,
					statusText: response.statusText,
					url: response.url
				});
				throw new ApiError(response.status, await response.text());
			}

			return response.json();
		} catch (error) {
			console.error(`API Error: ${fullUrl}`, error);
			throw error;
		}
	}
}