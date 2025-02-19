<!-- src/lib/components/homepage/artwork/ArtworkModal.svelte -->
<script lang="ts">
	import { onMount } from 'svelte';
	import type { Artwork } from '$lib/types/artwork';
	import { Button } from '$lib/components/ui/button';
	import ArtworkImage from '$lib/components/shared/ArtworkImage.svelte';
	
	export let artwork: Artwork;
	export let isOpen = false;
	export let onClose: () => void;

	// Handle escape key to close modal
	function handleKeydown(event: KeyboardEvent) {
		if (event.key === 'Escape' && isOpen) {
			onClose();
		}
	}

	// Click outside to close
	function handleBackdropClick(event: MouseEvent) {
		if (event.target === event.currentTarget) {
			onClose();
		}
	}

	// Focus trap and management
	let modalContent: HTMLDivElement;

	onMount(() => {
		if (isOpen) {
			modalContent?.focus();
		}
	});
</script>

{#if isOpen}
	<div
		class="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4 sm:p-6"
		on:click={handleBackdropClick}
		on:keydown={handleKeydown}
		role="presentation"
	>
		<div
			bind:this={modalContent}
			class="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto focus:outline-none"
			tabindex="-1"
			role="dialog"
			aria-labelledby="artwork-modal-title"
			aria-modal="true"
		>
			<!-- Image container -->
			<div class="bg-muted p-6 flex items-center justify-center h-[80vh]">
				<ArtworkImage
					primaryUrl={artwork.primaryImageUrl ?? null}
					thumbnailUrl={artwork.thumbnailImageUrl ?? null}
					alt={artwork.title}
					className="max-w-[90%] max-h-[90%] object-contain"
					objectFit="contain"
				/>
			</div>

			<!-- Details section -->
			<div class="p-6 space-y-4">
				<div>
					<h2 id="artwork-modal-title" class="text-2xl font-bold">
						{artwork.title}
					</h2>
					<p class="text-lg text-muted-foreground">
						{artwork.fullAttribution}
						{artwork.creationDate ? ` • ${artwork.creationDate}` : ''}
					</p>
				</div>

				<div class="grid gap-2">
					{#if artwork.classification}
						<div class="flex gap-2">
							<span class="font-medium">Type:</span>
							<span>{artwork.classification}</span>
						</div>
					{/if}

					{#if artwork.medium}
						<div class="flex gap-2">
							<span class="font-medium">Medium:</span>
							<span>{artwork.medium}</span>
						</div>
					{/if}

					{#if artwork.culture || artwork.country || artwork.region}
						<div class="flex gap-2 items-center">
        			<span class="font-medium">
								{#if artwork.culture && (artwork.country || artwork.region)}
										Culture and Region:
								{:else if artwork.culture}
										Culture:
								{:else}
										Region:
								{/if}
        			</span>
							<span>
            		{#if artwork.culture}
                	{artwork.culture}
            		{/if}
									{#if artwork.culture && (artwork.country || artwork.region)}
                		{' • '}
            			{/if}
								{#if artwork.country || artwork.region}
                	{[artwork.region, artwork.country]
										.filter(Boolean)
										.join(", ")}
            		{/if}
        			</span>
						</div>
					{/if}

					<div class="flex gap-2">
						<span class="font-medium">Location:</span>
						<span>
							{artwork.museum?.name}
							{' • Gallery ' + artwork.galleryNumber}
						</span>
					</div>
				</div>

				<Button
					variant="secondary"
					class="mt-6"
					onclick={onClose}
				>
					Close
				</Button>
			</div>
		</div>
	</div>
{/if}