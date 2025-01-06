<!-- src/lib/components/homepage/artwork/ArtworkModal.svelte -->
<script lang="ts">
	import { onMount } from 'svelte';
	import type { Artwork } from '$lib/types/artwork';
	
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
		class="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center"
		on:click={handleBackdropClick}
		on:keydown={handleKeydown}
		role="presentation"
	>
		<div
			bind:this={modalContent}
			class="bg-white rounded-lg max-w-2xl w-full mx-4 overflow-hidden focus:outline-none"
			tabindex="-1"
			role="dialog"
			aria-labelledby="artwork-modal-title"
			aria-modal="true"
		>
			<div class="relative aspect-[4/3]">
				<img
					src={artwork.imageUrl}
					alt={artwork.title}
					class="w-full h-full object-cover"
				/>
				{#if artwork.isOnDisplay}
					<span class="absolute top-4 right-4 px-3 py-1 text-sm font-semibold rounded-full bg-green-100 text-green-800">
							On Display
					</span>
				{/if}
			</div>

			<div class="p-6">
				<h2 id="artwork-modal-title" class="text-2xl font-bold mb-2">
					{artwork.title}
				</h2>
				<p class="text-lg text-gray-600 mb-4">
					{artwork.artist}{artwork.year ? ` • ${artwork.year}` : ''}
				</p>

				<button
					type="button"
					class="mt-4 px-4 py-2 bg-gray-200 hover:bg-gray-300 rounded-lg transition-colors"
					on:click={onClose}
				>
					Close
				</button>
			</div>
		</div>
	</div>
{/if}