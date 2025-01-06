<!-- src/lib/components/tour/TourHeader.svelte -->
<script lang="ts">
	import { goto } from '$app/navigation';
	import { ArrowLeft } from 'lucide-svelte';

	// We'll only accept the essential information needed for the header
	interface TourHeaderProps {
		name: string;
		estimatedDuration: number;
	}

	export let tour: TourHeaderProps;
	export let returnUrl: string = '/?tab=tours';  // Default to homepage

	/*// Helper function to format the duration in a readable way
	// For example, "1 hour 30 minutes" instead of just "90 minutes"
	function formatDuration(minutes: number): string {
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;

		if (hours === 0) {
			return `${minutes} minutes`;
		} else if (remainingMinutes === 0) {
			return `${hours} ${hours === 1 ? 'hour' : 'hours'}`;
		} else {
			return `${hours} ${hours === 1 ? 'hour' : 'hours'} ${remainingMinutes} minutes`;
		}
	}*/

	// Navigation handler for the back button
	function handleBack() {
		// This will return the user to the tours list
		goto(returnUrl);
	}
</script>

<header class="bg-white border-b border-gray-200">
	<!-- Container to maintain consistent width and padding -->
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
		<!-- Back button and title row -->
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

		<!-- Duration display -->
		<!--<div class="flex items-center text-gray-600">
			<Clock class="w-5 h-5 mr-2" />
			<span class="text-sm">
                Estimated duration: {formatDuration(tour.estimatedDuration)}
			</span>
		</div>-->
	</div>
</header>