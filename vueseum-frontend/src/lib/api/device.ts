// src/lib/api/device.ts
import { API_BASE_URL } from '$lib/config';

async function getDeviceFingerprint(): Promise<string> {
	if (import.meta.env.DEV) {
		return 'dev-device-fingerprint-123';
	}

	const headers = new Headers({
		'User-Agent': navigator.userAgent,
		'X-Screen-Resolution': `${window.screen.width}x${window.screen.height}`,
		'X-Timezone': Intl.DateTimeFormat().resolvedOptions().timeZone,
		'Accept-Language': navigator.languages.join(',')
	});

	const response = await fetch(`${API_BASE_URL}/device/fingerprint`, {
		headers
	});

	if (!response.ok) {
		throw new Error('Failed to get device fingerprint');
	}

	return response.text();
}

// Cache the fingerprint
let cachedFingerprint: string | null = null;

export async function getOrCreateFingerprint(): Promise<string> {
	if (!cachedFingerprint) {
		cachedFingerprint = await getDeviceFingerprint();
	}
	return cachedFingerprint;
}