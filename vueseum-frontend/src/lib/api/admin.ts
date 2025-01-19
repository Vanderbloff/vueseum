// src/lib/api/admin.ts
import { BaseApiClient } from './base';
import { API_BASE_URL } from '../config';

export interface SyncStatus {
	totalArtworksInDb: number;
	processingErrors: number;
	syncStartTime: string;
	processedCount: number;
}

export class AdminApiClient extends BaseApiClient {
	private readonly baseUrl = `${API_BASE_URL}/admin`;

	async startSync(): Promise<void> {
		await this.fetchWithError(
			`${this.baseUrl}/sync/start`,
			{ method: 'POST' }
		);
	}

	async getSyncStatus(): Promise<SyncStatus> {
		return this.fetchWithError(
			`${this.baseUrl}/sync/status`
		);
	}
}

export const adminApi = new AdminApiClient();