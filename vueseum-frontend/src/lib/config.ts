export const API_BASE_URL = (() => {
	if (import.meta.env.DEV) {
		return 'http://localhost:3001';
	}

	// During SSR, use relative URL
	if (typeof window === 'undefined') {
		return '/api/v1';
	}

	// In browser, use absolute URL
	return `${window.location.origin}/api/v1`;
})();