// Create new file: src/lib/api/base.ts
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
			const response = await fetch(endpoint, {
				...options,
				headers: {
					'Content-Type': 'application/json',
					...options.headers,
				},
			});

			if (!response.ok) {
				throw new ApiError(response.status, await response.text());
			}

			return response.json();
		} catch (error) {
			console.error(`API Error: ${endpoint}`, error);
			throw error;
		}
	}
}