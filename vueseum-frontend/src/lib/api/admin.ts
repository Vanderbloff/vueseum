// src/lib/api/admin.ts
import { BaseApiClient } from './base';

export interface SyncStatus {
	totalArtworksInDb: number;
	processingErrors: number;
	syncStartTime: string;
	processedCount: number;
}

export class AdminApiClient extends BaseApiClient {
	constructor() {
		super('/admin');
	}

	async startSync(): Promise<void> {
		await this.fetchWithError(
			'/sync/start',
			{ method: 'POST' }
		);
	}

	async getSyncStatus(): Promise<SyncStatus> {
		return this.fetchWithError('/sync/status');
	}
}

export const adminApi = new AdminApiClient();