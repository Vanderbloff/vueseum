<!-- src/lib/components/homepage/artwork/ArtworkCard.svelte -->
<script lang="ts">
	import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "$lib/components/ui/card";
	import { AspectRatio } from "$lib/components/ui/aspect-ratio";
	import type { Artwork } from '$lib/types/artwork';

	export let artwork: Artwork;
	export let onCardClick: (art: typeof artwork) => void;
</script>

<Card
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
		<p class="text-sm text-muted-foreground">
			{artwork.artist}
		</p>
		<p class="text-sm text-muted-foreground">
			{artwork.year}
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