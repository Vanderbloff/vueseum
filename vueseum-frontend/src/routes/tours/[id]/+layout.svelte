<script lang="ts">
	import { navigating } from '$app/state';
	import TourDetailSkeleton from '$lib/components/tour/TourDetailSkeleton.svelte';

	let { children } = $props();
	const isLoading = $derived(
		navigating?.to?.route.id === '/tours/[id]' &&
		navigating?.from?.route.id !== '/tours/[id]'
	);
</script>

<div class="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
	{#if isLoading}
		<TourDetailSkeleton />
	{:else}
		{@render children?.()}
	{/if}
</div>