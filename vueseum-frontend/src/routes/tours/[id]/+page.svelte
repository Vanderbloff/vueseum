<!-- src/routes/tours/[id]/+page.svelte -->
<script lang="ts">
	import type { Tour } from '$lib/types/tour';
	import { Badge } from "$lib/components/ui/badge";
	import { Separator } from "$lib/components/ui/separator";
	import TourView from '$lib/components/tour/TourView.svelte';
	import { goto } from '$app/navigation';
	import { ArrowLeft } from 'lucide-svelte';

	let { data } = $props();
	let returnUrl: string = '/?tab=tours';  // Default to tours tab

	const state = $state({
		loading: true,
		loadError: null as string | null,
		tour: null as Tour | null,
	});

	function handleBack() {
		// This will return the user to the tours list
		goto(returnUrl);
	}

	$effect(() => {
		if (data.tour) {
			state.tour = data.tour;
			state.loading = false;
		}
	});

</script>

<div class="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
	{#if state.loading}
		<!-- Skeleton UI matching new layout -->
		<div class="animate-pulse">
			<!-- Back button skeleton -->
			<div class="flex items-center mb-4 w-24">
				<div class="w-5 h-5 rounded bg-gray-200"></div>
				<div class="ml-2 h-4 w-16 bg-gray-200 rounded"></div>
			</div>

			<!-- Header section skeleton -->
			<div class="space-y-3 mb-4">
				<!-- Title skeleton -->
				<div class="h-7 bg-gray-200 rounded w-3/4"></div>  <!-- Adjusted height for text-2xl -->

				<!-- Theme badge skeleton -->
				<div class="w-32 h-6 bg-gray-200 rounded-full"></div>

				<!-- Description skeleton -->
				<div class="space-y-2">
					<div class="h-4 bg-gray-200 rounded w-full"></div>
					<div class="h-4 bg-gray-200 rounded w-5/6"></div>
					<div class="h-4 bg-gray-200 rounded w-4/6"></div>
				</div>
			</div>

			<div class="h-px bg-gray-200 my-4"></div> <!-- Separator skeleton -->

			<!-- Carousel skeleton -->
			<div class="mt-8">
				<div class="space-y-2 mb-6">
					<!-- Progress indicators skeleton -->
					<div class="flex justify-between text-sm">
						<div class="w-24 h-4 bg-gray-200 rounded"></div>
						<div class="w-24 h-4 bg-gray-200 rounded"></div>
					</div>
					<div class="h-2 bg-gray-200 rounded-full"></div>
				</div>

				<!-- Carousel card skeleton -->
				<div class="bg-white rounded-lg shadow-md overflow-hidden">
					<div class="aspect-[4/3] bg-gray-200"></div>
					<div class="p-6 space-y-4">
						<div class="space-y-2">
							<div class="h-8 bg-gray-200 rounded w-3/4"></div>
							<div class="h-4 bg-gray-200 rounded w-1/2"></div>
						</div>
						<div class="space-y-2">
							<div class="h-4 bg-gray-200 rounded w-full"></div>
							<div class="h-4 bg-gray-200 rounded w-5/6"></div>
							<div class="h-4 bg-gray-200 rounded w-4/6"></div>
						</div>
					</div>
				</div>
			</div>
		</div>
	{:else if state.loadError}
		<div class="text-center text-red-600 py-8">
			<p>{state.loadError}</p>
			<button
				class="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
				onclick={() => window.location.reload()}
			>
				Try Again
			</button>
		</div>
	{:else if state.tour}
		<!-- Back navigation -->
		<button
			onclick={handleBack}
			class="inline-flex items-center text-gray-600 mb-6 hover:text-gray-900"
		>
			<ArrowLeft class="h-5 w-5" />
			<span class="ml-2">Back</span>
		</button>

		<!-- Tour header section -->
		<div class="space-y-3 mb-4">
			<h1 class="text-2xl font-bold">{state.tour.name}</h1>
			<Badge variant="secondary" class="text-sm font-medium">{state.tour.theme}</Badge>
			<p class="text-gray-700 leading-snug">{state.tour.description}</p>
		</div>

		<Separator class="my-4" />

		<TourView tour={state.tour} />
	{:else}
		<!-- Not found state -->
	{/if}
</div>