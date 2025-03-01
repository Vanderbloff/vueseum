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

		const headerEntries: Record<string, string> = {
			'Content-Type': 'application/json',
			...(options.headers as Record<string, string> || {})
		};

		// Add CSRF token for non-GET requests
		if (options.method && options.method !== 'GET') {
			const token = this.getCsrfToken();
			if (token) {
				console.log('Adding CSRF token to request');
				headerEntries['X-XSRF-TOKEN'] = token;
			} else {
				console.warn('No CSRF token found for non-GET request');
			}
		}

		const requestOptions: RequestInit = {
			...options,
			credentials: 'include',
			headers: headerEntries
		};

		try {
			const response = await fetch(fullUrl, requestOptions);

			if (!response.ok) {
				console.log('Response not OK:', {
					status: response.status,
					statusText: response.statusText,
					url: response.url
				});

				let errorMessage: string;
				try {
					const errorData = await response.json();
					errorMessage = errorData.message || errorData.error || response.statusText;
				} catch {
					errorMessage = await response.text();
				}

				throw new ApiError(response.status, errorMessage);
			}

			return response.json();
		} catch (error) {
			if (error instanceof ApiError) {
				throw error;
			}
			console.error(`API Error: ${fullUrl}`, error);
			throw error;
		}
	}

	private getCsrfToken(): string {
		try {
			if (process.env.NODE_ENV !== 'production') {
				console.debug('Available cookies:', document.cookie);
			}

			const cookies = document.cookie.split(';');
			for (const cookie of cookies) {
				const [name, value] = cookie.trim().split('=');
				if (name === 'XSRF-TOKEN') {
					return decodeURIComponent(value);
				}
			}
		} catch (error) {
			console.error('Error extracting CSRF token:', error);
		}
		return '';
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