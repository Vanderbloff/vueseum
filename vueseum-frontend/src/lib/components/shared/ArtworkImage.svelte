<script lang="ts">
	import { onMount } from 'svelte';
	import { Skeleton } from "$lib/components/ui/skeleton";

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

	// Helper function to get proxied URL
	function getProxiedUrl(url: string | null): string | null {
		if (!url) return null;
		try {
			// First decode in case the URL comes pre-encoded
			const decodedUrl = decodeURIComponent(url);
			// Then do a single clean encode
			return `/api/v1/images/proxy?url=${encodeURIComponent(decodedUrl)}`;
		} catch (error) {
			console.error('URL encoding error:', error);
			return null;
		}
	}

	// Validate image URL and handle fallback
	/*async function validateImage(url: string): Promise<boolean> {
		if (!url) return false;
		try {
			const response = await fetch(url, {
				method: 'HEAD'
			});
			return response.ok;
		} catch (error) {
			console.error('Image validation error:', error);
			return false;
		}
	}*/

	onMount(async () => {
		const primaryProxyUrl = getProxiedUrl(primaryUrl);
		const thumbnailProxyUrl = getProxiedUrl(thumbnailUrl);

		console.log('primaryProxyUrl:', primaryProxyUrl);
		console.log('thumbnailProxyUrl:', thumbnailProxyUrl);

		try {
			if (primaryProxyUrl) {
				const response = await fetch(primaryProxyUrl, { method: 'HEAD' });
				if (response.ok) {
					state.currentUrl = primaryUrl;
					return;
				}
			}

			if (thumbnailProxyUrl) {
				const response = await fetch(thumbnailProxyUrl, { method: 'HEAD' });
				if (response.ok) {
					state.currentUrl = thumbnailUrl;
					return;
				}
			}

			state.hasError = true;
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			state.hasError = true;
		}

		state.isLoading = false;
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
			src={getProxiedUrl(state.currentUrl)}
			{alt}
			class="max-w-full max-h-full w-auto h-auto {className}"
			style="object-fit: {objectFit};"
			onerror={() => state.hasError = true}
		/>
	</div>
{/if}