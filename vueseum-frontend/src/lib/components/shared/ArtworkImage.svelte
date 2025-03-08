<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { onDestroy } from 'svelte';

	let {
		primaryUrl,
		thumbnailUrl,
		alt,
		className = '',
		objectFit = 'contain'
	} = $props<{
		primaryUrl: string | null;
		thumbnailUrl: string | null;
		alt: string;
		className?: string;
		objectFit?: 'contain' | 'cover';
	}>();

	const state = $state({
		currentUrl: null as string | null,
		isLoading: true,
		hasError: false
	});

	function getProxiedUrl(url: string | null): string | null {
		if (!url) return null;
		return `/api/v1/images/proxy?url=${encodeURIComponent(url)}`;
	}

	async function tryLoadImage(url: string | null) {
		if (!url) {
			state.hasError = true;
			state.isLoading = false;
			return;
		}

		state.isLoading = true;
		state.hasError = false;

		const proxiedUrl = getProxiedUrl(url);
		if (!proxiedUrl) {
			handleImageFailure(url);
			return;
		}

		const timeoutPromise = new Promise((_, reject) => {
			setTimeout(() => reject(new Error('Image loading timeout')), 5000);
		});

		try {
			const response = await Promise.race([
				fetch(proxiedUrl),
				timeoutPromise
			]) as Response;

			if (!response.ok) {
				handleImageFailure(url);
				return;
			}

			const blob = await response.blob();
			if (blob.size === 0) {
				handleImageFailure(url);
				return;
			}

			state.currentUrl = URL.createObjectURL(blob);
			state.isLoading = false;
		} catch {
			handleImageFailure(url);
		}
	}

	function handleImageFailure(failedUrl: string) {
		if (failedUrl === primaryUrl && thumbnailUrl) {
			tryLoadImage(thumbnailUrl);
		} else {
			state.hasError = true;
			state.isLoading = false;
		}
	}

	// Initial load attempt
	$effect(() => {
		if (primaryUrl) {
			tryLoadImage(primaryUrl);
		} else if (thumbnailUrl) {
			tryLoadImage(thumbnailUrl);
		} else {
			state.hasError = true;
			state.isLoading = false;
		}
	});

	// Cleanup
	onDestroy(() => {
		if (state.currentUrl?.startsWith('blob:')) {
			URL.revokeObjectURL(state.currentUrl);
		}
	});
</script>

{#if state.isLoading}
	<Skeleton class="w-full h-full" />
{:else if state.hasError || !state.currentUrl}
	<div class="w-full h-full bg-muted flex items-center justify-center">
		<span class="text-muted-foreground">Image unavailable</span>
	</div>
{:else}
	<div class="w-full h-full overflow-hidden flex items-center justify-center">
		<img
			src={state.currentUrl}
			{alt}
			class="max-w-full max-h-full w-auto h-auto {className}"
			style="object-fit: {objectFit};"
		/>
	</div>
{/if}