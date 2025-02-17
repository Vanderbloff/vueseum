<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { adminApi, type SyncStatus } from '$lib/api/admin';
	import { onDestroy } from 'svelte';

	const state = $state({
		status: null as SyncStatus | null,
		loading: false,
		error: null as string | null,
		polling: false
	});

	async function startSync() {
		try {
			state.loading = true;
			state.error = null;
			await adminApi.startSync();
			// Start polling for status
			state.polling = true;
			await pollStatus();
		} catch (error) {
			state.error = error instanceof Error ?
				error.message :
				'Failed to start sync';
		} finally {
			state.loading = false;
		}
	}

	async function pollStatus() {
		if (!state.polling) return;

		try {
			state.status = await adminApi.getSyncStatus();
			// Continue polling if sync is still in progress
			if (state.status.processedCount > 0) {
				setTimeout(pollStatus, 5000); // Poll every 5 seconds
			} else {
				state.polling = false;
			}
		} catch (error) {
			state.error = error instanceof Error ?
				error.message :
				'Failed to get sync status';
			state.polling = false;
		}
	}

	onDestroy(() => {
		state.polling = false;
	});
</script>

<Card>
	<CardHeader>
		<CardTitle>Museum Sync Management</CardTitle>
	</CardHeader>
	<CardContent>
		<div class="space-y-4">
			<Button
				onclick={startSync}
				disabled={state.loading || state.polling}
			>
				{state.loading ? 'Starting Sync...' : 'Start Sync'}
			</Button>

			{#if state.error}
				<p class="text-destructive">{state.error}</p>
			{/if}

			{#if state.status}
				<div class="space-y-2">
					<p>Total Artworks: {state.status.totalArtworksInDb}</p>
					<p>Processed: {state.status.processedCount}</p>
					<p>Errors: {state.status.processingErrors}</p>
					<p>Started: {new Date(state.status.syncStartTime).toLocaleString()}</p>
				</div>
			{/if}
		</div>
	</CardContent>
</Card>