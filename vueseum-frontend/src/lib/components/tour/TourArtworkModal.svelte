<!-- src/lib/components/tour/TourArtworkModal.svelte -->
<script lang="ts">
	import { Button } from "$lib/components/ui/button";
	import { X } from "lucide-react";

	interface TourArtwork {
		id: number;
		title: string;
		imageUrl: string;
	}

	let { artwork, isOpen, onClose } = $props<{
		artwork: TourArtwork;
		isOpen: boolean;
		onClose: () => void;
	}>();

	$effect(() => {
		if (isOpen) {
			// Log exact DOM position
			const modalElement = document.querySelector('[data-modal-debug]');
			console.log('Modal element:', modalElement);
			console.log('Modal element parent:', modalElement?.parentElement);
		}
	});
</script>

{#if isOpen}
	<div
		data-modal-debug
		class="fixed inset-0 bg-red-500 flex items-center justify-center"
		style="position: fixed; z-index: 99999; top: 0; left: 0; right: 0; bottom: 0;"
	>
		<div class="relative max-w-4xl w-full mx-4">
			<img
				src={artwork.imageUrl}
				alt={artwork.title}
				class="w-full h-auto object-contain max-h-[90vh]"
			/>
			<Button
				variant="ghost"
				size="icon"
				class="absolute top-2 right-2 bg-black/50 hover:bg-black/75 text-white rounded-full"
				onclick={onClose}
			>
				<X class="h-4 w-4" />
			</Button>
		</div>
	</div>
{/if}