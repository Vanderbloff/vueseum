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
			return await this.fetchWithError('/fingerprint', {
				headers: {
					'User-Agent': navigator.userAgent,
					'X-Screen-Resolution': `${window.screen.width}x${window.screen.height}`,
					'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
					'Accept-Language': navigator.languages.join(',')
				}
			});
		} catch (error) {
			throw new Error('Failed to get device fingerprint');
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