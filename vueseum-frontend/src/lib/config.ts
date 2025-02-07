export const API_BASE_URL = (() => {
	if (import.meta.env.DEV) {
		return 'http://localhost:3001';
	}

	return 'https://vueseum-app-prod.eastus.cloudapp.azure.com/api/v1';
})();