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

	/**
	 * Gets the CSRF token from cookies
	 * @returns The CSRF token or empty string if not found
	 */
	protected getCsrfToken(): string {
		try {
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

	/**
	 * Returns headers with CSRF token included when needed
	 * @param options Original request options
	 * @returns Headers object with CSRF token if applicable
	 */
	protected getHeadersWithCsrf(options: RequestInit = {}): Record<string, string> {
		const headers: Record<string, string> = {
			'Content-Type': 'application/json',
			...(options.headers as Record<string, string> || {})
		};

		// Add CSRF token for non-GET/HEAD requests
		if (options.method && !['GET', 'HEAD'].includes(options.method)) {
			const token = this.getCsrfToken();
			if (token) {
				headers['X-XSRF-TOKEN'] = token;
			} else {
				console.warn('No CSRF token found for non-GET request');
			}
		}

		return headers;
	}

	protected async fetchWithError<T>(
		path: string,
		options: RequestInit = {}
	): Promise<T> {
		const fullUrl = `${API_BASE_URL}/api/v1${this.basePath}${path}`;

		const requestOptions: RequestInit = {
			...options,
			credentials: 'include',
			headers: this.getHeadersWithCsrf(options)
		};

		try {
			const response = await fetch(fullUrl, requestOptions);
			const responseText = await response.text();

			if (!response.ok) {
				let errorMessage: string;
				try {
					const errorData = JSON.parse(responseText);
					errorMessage = errorData.message || errorData.error || response.statusText;
				} catch {
					// If parsing fails, use the raw text
					errorMessage = responseText;
				}

				throw new ApiError(response.status, errorMessage);
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