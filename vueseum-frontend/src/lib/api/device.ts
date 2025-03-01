import { BaseApiClient } from './base';
import { API_BASE_URL } from '$lib/config';

export class DeviceFingerprintClient extends BaseApiClient {
	constructor() {
		super('/device');
	}

	async getFingerprint(): Promise<string> {
		if (import.meta.env.DEV) {
			return 'dev-device-fingerprint-123';
		}

		try {
			console.log('Attempting to get device fingerprint...');

			const fullUrl = `${API_BASE_URL}/api/v1${this.basePath}/fingerprint`;

			const response = await fetch(fullUrl, {
				method: 'GET',
				headers: {
					'User-Agent': navigator.userAgent,
					'X-Screen-Resolution': `${window.screen.width}x${window.screen.height}`,
					'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
					'Accept-Language': navigator.languages.join(',')
				},
				credentials: 'include'
			});

			if (!response.ok) {
				throw new Error(`Failed to get fingerprint: ${response.status}`);
			}

			const data = await response.json();
			console.log('Fingerprint response:', data);

			return data.fingerprint;
		} catch (error) {
			console.error('Device fingerprint error details:', error);
			throw error;
		}
	}
}

// Cache the fingerprint
let cachedFingerprint: string | null = null;

export async function getOrCreateFingerprint(): Promise<string> {
	if (!cachedFingerprint) {
		const client = new DeviceFingerprintClient();
		cachedFingerprint = await client.getFingerprint();
	}
	return cachedFingerprint;
}