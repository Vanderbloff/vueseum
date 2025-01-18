<!-- src/lib/components/tour/TourStopCard.svelte -->
<script lang="ts">
	import type { TourStop } from '$lib/types/tour';
	export let stop: TourStop;
</script>

<!-- The card layout uses a consistent structure for all stops -->
<article class="bg-card rounded-lg shadow-md overflow-hidden border border-border">
	<!-- Progress indicator at top -->
	<div class="text-sm text-muted-foreground p-2 bg-muted/50 border-b border-border">
		Stop {stop.sequenceNumber}
	</div>

	<!-- Visual hierarchy section -->
	<div class="flex flex-col p-4 gap-4">
		<!-- Title and core details -->
		<div>
			<h3 class="text-2xl font-semibold text-foreground">
				{stop.artwork.title}
			</h3>
			<div class="mt-2 space-y-1">
				<!-- Artist attribution -->
				<p class="text-lg text-foreground">
					{stop.artwork.fullAttribution}
				</p>

				<!-- Key details -->
				<div class="space-y-0.5 text-muted-foreground">
					{#if stop.artwork.creationDate}
						<p>{stop.artwork.creationDate}</p>
					{/if}
					{#if stop.artwork.medium}
						<p>{stop.artwork.medium}</p>
					{/if}
					{#if stop.artwork.classification}
						<p>{stop.artwork.classification}</p>
					{/if}
				</div>
			</div>
		</div>

		<!-- Cultural and geographic context -->
		{#if stop.artwork.culture || stop.artwork.country || stop.artwork.region}
			<div class="space-y-1 text-sm">
				{#if stop.artwork.culture}
					<p class="text-foreground">
						<span class="text-muted-foreground">Culture:</span> {stop.artwork.culture}
					</p>
				{/if}
				{#if stop.artwork.country || stop.artwork.region}
					<p class="text-foreground">
						<span class="text-muted-foreground">Location:</span>
						{[stop.artwork.region, stop.artwork.country].filter(Boolean).join(", ")}
					</p>
				{/if}
			</div>
		{/if}
	</div>

	<!-- Artwork image -->
	<div class="relative aspect-[16/9]">
		{#if stop.artwork.imageUrl}
			<img
				src={stop.artwork.imageUrl}
				alt={stop.artwork.title}
				class="absolute inset-0 w-full h-full object-cover"
				loading="lazy"
			/>
		{:else}
			<div class="absolute inset-0 bg-muted flex items-center justify-center">
				<span class="text-muted-foreground">No image available</span>
			</div>
		{/if}
	</div>

	<!-- Museum context -->
	<div class="px-4 py-3 bg-muted/50 border-y border-border">
		<div class="flex flex-wrap gap-x-3 gap-y-1 text-sm">
			{#if stop.artwork.galleryNumber}
				<p class="text-foreground">
					<span class="font-medium">Gallery {stop.artwork.galleryNumber}</span>
				</p>
			{/if}
			{#if stop.artwork.department}
				<p class="text-muted-foreground italic">
					{stop.artwork.galleryNumber ? '•' : ''} {stop.artwork.department}
				</p>
			{/if}
		</div>
	</div>

	<!-- Description section -->
	<div class="p-4 space-y-3">
		<!-- Tour-specific description -->
		<p class="text-foreground">
			{stop.tourContextDescription}
		</p>

		<!-- Artwork description -->
		{#if stop.artwork.description}
			<p class="text-sm text-muted-foreground">
				{stop.artwork.description}
			</p>
		{/if}
	</div>
</article>