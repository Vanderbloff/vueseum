<!--src/lib/components/tour/TourCard.svelte-->
<script lang="ts">
	import { Clock, MapPin } from 'lucide-svelte';
	import { goto } from '$app/navigation';

	interface Tour {
		id: number;
		name: string;
		description: string;
		theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
		estimatedDuration: number;
		museum: {
			name: string;
			location: string;
		};
	}

	export let tour: Tour;
	// Make onSelect optional with a default handler
	export let onSelect: (tour: Tour) => void = (tour) => {
		goto(`/tours/${tour.id}`);
	};

	function formatDuration(minutes: number): string {
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;
		return hours > 0
			? `${hours}h ${remainingMinutes}m`
			: `${remainingMinutes}m`;
	}

	// Helper to make theme more readable
	function formatTheme(theme: Tour['theme']): string {
		return theme.split('_')
			.map(word => word.charAt(0) + word.slice(1).toLowerCase())
			.join(' ');
	}
</script>

<button
	class="w-full p-4 rounded-lg bg-white shadow-md hover:shadow-lg transition-shadow duration-200 text-left"
	on:click={() => onSelect(tour)}
>
	<div class="flex flex-col gap-2">
		<h3 class="text-lg font-semibold text-gray-900">{tour.name}</h3>
		<p class="text-sm text-gray-600 line-clamp-2">{tour.description}</p>

		<div class="flex items-center gap-4 mt-2 text-sm text-gray-500">
			<div class="flex items-center gap-1">
				<Clock class="w-4 h-4" />
				<span>{formatDuration(tour.estimatedDuration)}</span>
			</div>
			<div class="flex items-center gap-1">
				<MapPin class="w-4 h-4" />
				<span>{formatTheme(tour.theme)}</span>
			</div>
		</div>

		<div class="text-sm text-gray-500 mt-1">
			{tour.museum.name}
		</div>
	</div>
</button>