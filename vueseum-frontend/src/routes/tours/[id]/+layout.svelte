<script lang="ts">
	import { navigating } from '$app/state';
	import TourDetailSkeleton from '$lib/components/tour/TourDetailSkeleton.svelte';

	let { children } = $props();

</script>

<div class="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
	{#if navigating === null || Object.values(navigating).every(v => v === null)}
		{@render children?.()}
	{:else if navigating}
		<TourDetailSkeleton />
	{:else}
		{@render children?.()}
	{/if}
</div>