<script lang="ts">
	import { onMount } from 'svelte';
	import { Skeleton } from "$lib/components/ui/skeleton";

	let {
		primaryUrl,
		thumbnailUrl,
		alt,
		className = ''
	} = $props<{
		primaryUrl: string | null;
		thumbnailUrl: string | null;
		alt: string;
		className?: string;
	}>();

	const state = $state({
		currentUrl: primaryUrl || thumbnailUrl,
		isLoading: true,
		hasError: false
	});

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
		if (primaryUrl && await validateImage(primaryUrl)) {
			state.currentUrl = primaryUrl;
		}
		// Fall back to thumbnail if primary fails
		else if (thumbnailUrl && await validateImage(thumbnailUrl)) {
			state.currentUrl = thumbnailUrl;
		}
		else {
			state.hasError = true;
		}
		state.isLoading = false;
	});
</script>

{#if state.isLoading}
	<Skeleton class={`w-full h-full ${className}`} />
{:else if state.hasError || !state.currentUrl}
	<div class="w-full h-full bg-muted flex items-center justify-center">
		<span class="text-muted-foreground">Image unavailable</span>
	</div>
{:else}
	<img
		src={state.currentUrl}
		{alt}
		class={`object-cover w-full h-full ${className}`}
		onerror={() => state.hasError = true}
	/>
{/if}