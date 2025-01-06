<!-- src/lib/components/tour/TourStopList.svelte -->
<script lang="ts">
	import TourStopCard from './TourStopCard.svelte';
	import type { TourStop } from '$lib/types/tour';

	// This will be passed from TourDetailPage
	export let stops: TourStop[];
</script>

<div class="relative max-w-3xl mx-auto px-4 py-8">
	<!-- Container for the connecting line -->
	<div
		class="absolute left-8 top-12 bottom-12 w-0.5 bg-gray-200"
		aria-hidden="true"
	></div>

	<!-- List of tour stops -->
	<ul class="relative space-y-8">
		{#each stops as stop (stop.sequenceNumber)}
			<li class="relative">
				<!-- We wrap each TourStopCard in a container for positioning -->
				<div class="ml-8">
					<!-- Small dot connecting to the vertical line -->
					<div
						class="absolute -left-2 mt-6 w-4 h-4 rounded-full bg-blue-500 border-4 border-white"
						aria-hidden="true"
					></div>

					<TourStopCard {stop} />
				</div>
			</li>
		{/each}
	</ul>
</div>

<style>
    /* Ensure the connecting line stays behind the cards */
    .relative {
        z-index: 1;
    }

    /* The dots and line should be below the cards */
    .absolute {
        z-index: 0;
    }
</style>