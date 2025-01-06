<!-- src/lib/components/tour/TourDescription.svelte -->
<script lang="ts">
	import { fade } from 'svelte/transition';
	import { ChevronDown, ChevronUp } from 'lucide-svelte';

	interface TourDescriptionProps {
		description: string;
		theme: "CHRONOLOGICAL" | "ARTIST_FOCUSED" | "CULTURAL";
	}

	export let tour: TourDescriptionProps;

	// State for expandable description
	let isExpanded = false;
	let showExpandButton = false;

	// Define our action properly
	function checkOverflow(node: HTMLElement) {
		const check = () => {
			// We're using line-height * 3 to determine if content exceeds 3 lines
			const lineHeight = parseFloat(getComputedStyle(node).lineHeight);
			const maxHeight = lineHeight * 3;
			showExpandButton = node.scrollHeight > maxHeight;
		};

		// Run initial check
		check();

		// Set up resize listener
		window.addEventListener('resize', check);

		// Cleanup
		return {
			destroy() {
				window.removeEventListener('resize', check);
			}
		};
	}

	function formatTheme(theme: string): string {
		return theme.split('_')
			.map(word => word.charAt(0) + word.slice(1).toLowerCase())
			.join(' ');
	}
</script>

<section
	class="bg-white px-4 py-6 sm:px-6 lg:px-8"
	transition:fade={{ duration: 200 }}
>
	<!-- Theme badge -->
	<div class="mb-4">
        <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
            {formatTheme(tour.theme)}
        </span>
	</div>

	<!-- Description text -->
	<div class="relative">
		<p
			use:checkOverflow
			class="text-gray-700 leading-relaxed"
			class:line-clamp-3={!isExpanded}
		>
			{tour.description}
		</p>

		<!-- Gradient overlay when text is truncated -->
		{#if showExpandButton && !isExpanded}
			<div class="absolute bottom-0 left-0 right-0 h-8 bg-gradient-to-t from-white to-transparent">
			</div>
		{/if}

		<!-- Expand/Collapse button -->
		{#if showExpandButton}
			<button
				on:click={() => isExpanded = !isExpanded}
				class="mt-2 flex items-center text-sm text-blue-600 hover:text-blue-800 transition-colors duration-200"
			>
				<span>{isExpanded ? 'Show less' : 'Read more'}</span>
				<svelte:component
					this={isExpanded ? ChevronUp : ChevronDown}
					class="w-4 h-4 ml-1"
				/>
			</button>
		{/if}
	</div>
</section>

<style>
    /* This class is actively used with class:line-clamp-3 directive */
    .line-clamp-3 {
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }
</style>