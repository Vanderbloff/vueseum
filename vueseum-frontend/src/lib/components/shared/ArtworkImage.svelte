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
			const response = await fetch(url, { method: 'HEAD' });
			return response.ok;
		} catch (error) {
			return false;
		}
	}

	onMount(async () => {
		// Try primary URL first
		if (primaryUrl && await validateImage(getProxiedUrl(primaryUrl)!)) {
			state.currentUrl = primaryUrl;
		}
		// Fall back to thumbnail if primary fails
		else if (thumbnailUrl && await validateImage(getProxiedUrl(thumbnailUrl)!)) {
			state.currentUrl = thumbnailUrl;
		}
		else {
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