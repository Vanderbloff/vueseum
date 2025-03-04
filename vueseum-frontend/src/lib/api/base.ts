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
		console.log(`Fetching: ${fullUrl}`);

		const headers: Record<string, string> = {
			'Content-Type': 'application/json',
			...(options.headers as Record<string, string> || {})
		};

		const requestOptions: RequestInit = {
			...options,
			credentials: 'include',
			headers
		};

		try {
			const response = await fetch(fullUrl, requestOptions);

			const responseText = await response.text();

			if (!response.ok) {
				console.log('Response not OK:', {
					status: response.status,
					statusText: response.statusText,
					url: response.url
				});

				let errorMessage: string;
				try {
					const errorData = JSON.parse(responseText);
					errorMessage = errorData.message || errorData.error || response.statusText;
				} catch {
					// If parsing fails, use the raw text
					errorMessage = responseText || response.statusText;
				}

				throw new ApiError(response.status, errorMessage);
			}

			if (!responseText) {
				return {} as T;
			}

			try {
				return JSON.parse(responseText) as T;
			} catch {
				console.warn('Failed to parse response as JSON, returning raw text');
				return responseText as unknown as T;
			}
		} catch (error) {
			if (error instanceof ApiError) {
				throw error;
			}
			console.error(`API Error: ${fullUrl}`, error);
			throw error;
		}
	}

	protected getEmptyPaginatedResponse<T>(size: number, page: number) {
		return {
			content: [] as T[],
			totalElements: 0,
			totalPages: 0,
			size,
			number: page
		};
	}
}