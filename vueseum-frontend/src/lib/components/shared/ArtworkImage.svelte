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
		currentUrl: primaryUrl || thumbnailUrl,
		isLoading: true,
		hasError: false
	});

	function getProxiedUrl(url: string | null): string | null {
		if (!url) return null;
		return `/api/v1/images/proxy?url=${encodeURIComponent(url)}`;
	}

	// Validate image URL and handle fallback
	async function validateImage(url: string): Promise<boolean> {
		if (!url) return false;
		try {
			const response = await fetch(url, {
				method: 'HEAD',
				headers: {
					'Accept': 'image/jpeg,image/png,image/*'
				}
			});
			console.log('Image validation response:', response.status, url);
			return response.ok;
		} catch (error) {
			console.error('Image validation error:', error);
			return false;
		}
	}

	onMount(async () => {
		console.log('Primary URL:', primaryUrl);
		console.log('Thumbnail URL:', thumbnailUrl);

		// Try primary URL first
		if (primaryUrl) {
			const proxiedUrl = getProxiedUrl(primaryUrl);
			console.log('Attempting primary URL:', proxiedUrl);
			if (await validateImage(proxiedUrl!)) {
				state.currentUrl = primaryUrl;
			}
		}

		// Fall back to thumbnail if primary fails
		if (!state.currentUrl && thumbnailUrl) {
			const proxiedUrl = getProxiedUrl(thumbnailUrl);
			console.log('Attempting thumbnail URL:', proxiedUrl);
			if (await validateImage(proxiedUrl!)) {
				state.currentUrl = thumbnailUrl;
			}
		}

		if (!state.currentUrl) {
			state.hasError = true;
			console.log('No valid image URL found');
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