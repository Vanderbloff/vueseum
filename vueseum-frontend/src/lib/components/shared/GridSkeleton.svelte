<!-- src/lib/components/shared/skeleton/GridSkeleton.svelte -->
<script lang="ts">
	import { Skeleton } from "$lib/components/ui/skeleton";

	interface SkeletonSizes {
		main: string;
		title: string;
		subtitle: string;
	}

	let props = $props();
	let count = props?.count ?? 6;
	let variant = props?.variant ?? 'default';
	let className = props?.className ?? '';

	// Helper function to determine skeleton heights based on variant
	function getSkeletonSizes(variant: string): SkeletonSizes {
		switch (variant) {
			case 'artwork':
				return {
					main: 'h-48',  // Taller for artwork images
					title: 'h-4',
					subtitle: 'h-3'
				};
			case 'tour':
				return {
					main: 'h-24',  // Shorter for tour cards
					title: 'h-4',
					subtitle: 'h-3'
				};
			default:
				return {
					main: 'h-32',
					title: 'h-4',
					subtitle: 'h-3'
				};
		}
	}

	const sizes = getSkeletonSizes(variant);
</script>

<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 {className}">
	{#each Array(count) as _, i (i)}
		<div class="space-y-4">
			<Skeleton class="w-full {sizes.main}" />
			<div class="space-y-2">
				<Skeleton class="w-3/4 {sizes.title}" />
				<Skeleton class="w-1/2 {sizes.subtitle}" />
			</div>
		</div>
	{/each}
</div>