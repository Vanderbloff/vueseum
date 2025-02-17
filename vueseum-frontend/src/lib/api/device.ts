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
			// Get screen resolution
			const screenResolution = `${window.screen.width}x${window.screen.height}`;

			// Get timezone
			const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

			// Get language preferences
			const languages = navigator.languages ?
				navigator.languages.join(',') :
				navigator.language || 'en';

			return await this.fetchWithError('/fingerprint', {
				headers: {
					'User-Agent': navigator.userAgent,
					'X-Screen-Resolution': screenResolution,
					'X-Timezone': timezone,
					'Accept-Language': languages
				}
			});
		} catch (error) {
			console.error('Device fingerprint error:', error);
			throw new Error('Failed to get device fingerprint. Please try again.');
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