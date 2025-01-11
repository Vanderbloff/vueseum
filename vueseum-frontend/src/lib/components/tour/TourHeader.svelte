<!-- src/lib/components/tour/TourHeader.svelte -->
<script lang="ts">
	import { goto } from '$app/navigation';
	import { ArrowLeft } from 'lucide-svelte';

	interface TourHeaderProps {
		name: string;
	}

	export let tour: TourHeaderProps;
	export let returnUrl: string = '/?tab=tours';  // Default to homepage

	// Navigation handler for the back button
	function handleBack() {
		// This will return the user to the tours list
		goto(returnUrl);
	}
</script>

<header class="bg-white border-b border-gray-200">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
		<div class="flex items-center mb-4">
			<button
				on:click={handleBack}
				class="mr-4 p-2 rounded-full hover:bg-gray-100 transition-colors duration-200"
				aria-label="Return to tours list"
			>
				<ArrowLeft class="w-5 h-5 text-gray-600" />
			</button>

			<h1 class="text-2xl font-bold text-gray-900">
				{tour.name}
			</h1>
		</div>
	</div>
</header>