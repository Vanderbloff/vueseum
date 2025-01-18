<!-- src/lib/components/homepage/artwork/ArtworkCard.svelte -->
<script lang="ts">
	import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "$lib/components/ui/card";
	import { AspectRatio } from "$lib/components/ui/aspect-ratio";
	import type { Artwork } from '$lib/types/artwork';

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
	<AspectRatio ratio={4/3} class="bg-muted">
		<img
			src={artwork.imageUrl}
			alt={artwork.title}
			class="object-cover w-full h-full transition-transform duration-200 group-hover:scale-105"
		/>
	</AspectRatio>

	<CardHeader class="p-4 pb-2">
		<CardTitle class="text-lg line-clamp-2">
			{artwork.title}
		</CardTitle>
	</CardHeader>

	<CardContent class="p-4 pt-0">
		<p class="text-sm text-muted-foreground flex items-center gap-1">
			{#if !artwork.isConfidentAttribution}
				<span
					class="text-muted-foreground/70 text-xs"
					title="Attribution uncertainty"
					aria-label="Attribution uncertainty indicator"
				>(?)</span>
			{/if}
			{artwork.artist}
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