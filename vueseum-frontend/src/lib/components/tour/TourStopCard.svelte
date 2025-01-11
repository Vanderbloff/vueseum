<!-- src/lib/components/tour/TourStopCard.svelte -->
<script lang="ts">
	import type { TourStop } from '$lib/types/tour';
	export let stop: TourStop;
</script>

<!-- The card layout uses a consistent structure for all stops -->
<article class="bg-white rounded-lg shadow-md overflow-hidden">
	<!-- Visual hierarchy: First the stop number, then artwork details -->
	<div class="flex items-start p-4">
		<!-- Stop number displayed prominently -->
		<div class="flex-shrink-0 w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mr-4">
            <span class="text-xl font-semibold text-blue-700">
                {stop.sequenceNumber}
            </span>
		</div>

		<!-- Artwork information section -->
		<div class="flex-grow">
			<h3 class="text-lg font-semibold text-gray-900">
				{stop.artwork.title}
			</h3>
			<p class="text-sm text-gray-600">
				{stop.artwork.artist}
			</p>
		</div>

	<!-- Artwork image with proper aspect ratio -->
	<div class="relative w-full pb-[56.25%]">
		<img
			src={stop.artwork.imageUrl}
			alt={stop.artwork.title}
			class="absolute inset-0 w-full h-full object-cover"
			loading="lazy"
		/>
	</div>

	<!-- Location information -->
	<div class="px-4 py-2 bg-gray-50 border-t border-gray-100">
		<p class="text-sm text-gray-600">
			{#if stop.artwork.galleryNumber}
				<strong>Gallery {stop.artwork.galleryNumber}</strong> <em>({stop.artwork.department})</em>
			{/if}
		</p>
	</div>

	<!-- Tour-specific description -->
	<div class="p-4">
		<p class="text-gray-700">
			{stop.description}
		</p>
	</div>
</article>