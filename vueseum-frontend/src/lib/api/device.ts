// src/lib/api/device.ts
import { BaseApiClient } from './base';

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

			const response = await this.fetchWithError<{ fingerprint: string }>('/fingerprint', {
				headers: {
					'User-Agent': navigator.userAgent,
					'X-Screen-Resolution': `${window.screen.width}x${window.screen.height}`,
					'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
					'Accept-Language': navigator.languages.join(',')
				}
			});

			console.log('Fingerprint response:', response);
			return response.fingerprint;
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