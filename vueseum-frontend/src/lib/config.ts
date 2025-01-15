export const API_BASE_URL = import.meta.env.DEV
	? 'http://localhost:3001'  // Development API URL
	: '/api/v1';              // Production API URL