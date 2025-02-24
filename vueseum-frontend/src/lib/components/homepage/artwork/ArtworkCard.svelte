<!-- src/lib/components/homepage/artwork/ArtworkCard.svelte -->
<script lang="ts">
	import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "$lib/components/ui/card";
	import { AspectRatio } from "$lib/components/ui/aspect-ratio";
	import type { Artwork } from '$lib/types/artwork';
	import ArtworkImage from '$lib/components/shared/ArtworkImage.svelte';

	export let artwork: Artwork;
	export let onCardClick: (art: typeof artwork) => void;
</script>

<Card
	data-testid="artwork-card"
	class="group overflow-hidden hover:shadow-lg transition-shadow duration-200 cursor-pointer"
	onclick={() => onCardClick(artwork)}
	role="button"
	onkeydown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onCardClick(artwork);
        }
    }}
>
	<AspectRatio ratio={1} class="bg-muted">
		<div class="w-full h-full flex items-center justify-center p-4">
			<ArtworkImage
				primaryUrl={artwork.primaryImageUrl ?? null}
				thumbnailUrl={artwork.thumbnailImageUrl ?? null}
				alt={artwork.title}
				className="transition-transform duration-200 group-hover:scale-105"
				objectFit="contain"
			/>
		</div>
	</AspectRatio>

	<CardHeader class="p-4 pb-2">
		<CardTitle class="text-lg line-clamp-2">
			{artwork.title}
		</CardTitle>
	</CardHeader>

	<CardContent class="p-4 pt-0">
		<p class="text-sm text-muted-foreground flex items-center gap-1">
			{#if !artwork.isConfidentAttribution}
				<span class="text-muted-foreground/70 text-xs" title="Attribution uncertainty" aria-label="Attribution uncertainty indicator">(?)</span>
			{/if}
			{artwork.fullAttribution || artwork.artist}
		</p>
		<p class="text-sm text-muted-foreground">
			{artwork.creationDate}
		</p>
	</CardContent>

	{#if artwork.department || artwork.medium}
		<CardFooter class="p-4 pt-0">
			<p class="text-xs text-muted-foreground">
				{[artwork.department, artwork.medium].filter(Boolean).join(' • ')}
			</p>
		</CardFooter>
	{/if}
</Card>