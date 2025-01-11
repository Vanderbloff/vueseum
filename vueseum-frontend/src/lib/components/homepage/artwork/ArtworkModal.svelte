<!-- src/lib/components/homepage/artwork/ArtworkModal.svelte -->
<script lang="ts">
	import { onMount } from 'svelte';
	import type { Artwork } from '$lib/types/artwork';
	import { Button } from '$lib/components/ui/button';
	
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
			<div class="relative">
				<div class="max-h-[50vh] sm:max-h-[60vh] bg-muted">
					<img
						src={artwork.imageUrl}
						alt={artwork.title}
						class="w-full h-full object-contain"
					/>
				</div>

				{#if artwork.isOnDisplay}
					<span class="absolute top-4 right-4 px-3 py-1 text-sm font-semibold rounded-full bg-green-100 text-green-800">
							On Display
					</span>
				{/if}
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
					{#if artwork.medium}
						<div class="flex gap-2">
							<span class="font-medium">Medium:</span>
							<span>{artwork.medium}</span>
						</div>
					{/if}

					{#if artwork.department}
						<div class="flex gap-2">
							<span class="font-medium">Department:</span>
							<span>{artwork.department}</span>
						</div>
					{/if}

					{#if artwork.geographicLocation}
						<div class="flex gap-2">
							<span class="font-medium">Culture:</span>
							<span>{artwork.geographicLocation}</span>
						</div>
					{/if}

					{#if artwork.galleryNumber}
						<div class="flex gap-2">
							<span class="font-medium">Location:</span>
							<span>Gallery {artwork.galleryNumber}</span>
						</div>
					{/if}
				</div>

				<!-- Using shadcn Button component -->
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