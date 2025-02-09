<!-- src/routes/tours/[id]/+page.svelte -->
<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { Separator } from '$lib/components/ui/separator';
	import TourView from '$lib/components/tour/TourView.svelte';
	import { Button } from '$lib/components/ui/button';
	import { goto } from '$app/navigation';
	import { ArrowLeft } from 'lucide-svelte';
	import { page } from '$app/state';
	import type { PageData } from './$types';
	import { tourApi } from '$lib/api/tour';
	import type { Tour } from '$lib/types/tour';

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isLoading: false,
		error: null as string | null,
		tour: null as Tour | null
	});

	function handleBack() {
		// If we came from the tours list, go back
		if (document.referrer.includes('/?tab=tours')) {
			window.history.back();
		} else {
			// Otherwise, go to tours list
			goto('/?tab=tours');
		}
	}

	$effect(() => {
		if (page.error) {
			console.error('Tour page error:', page.error);
		}
	});

	async function loadTour(tourId: string) {
		try {
			state.isLoading = true;
			const parsedId = parseInt(tourId);
			if (isNaN(parsedId)) {
				throw new Error('Invalid tour ID');
			}

			state.tour = await tourApi.getTourById(parsedId);
			state.error = null;
		} catch (e) {
			console.error('Error loading tour:', e);
			state.error = e instanceof Error ? e.message : 'An unexpected error occurred';
			state.tour = null;
		} finally {
			state.isLoading = false;
		}
	}

	// Effect triggers load but doesn't try to handle async directly
	$effect(() => {
		if (typeof window === 'undefined') return;
		loadTour(data.tourId);
	});
</script>

{#if data.loadError}
	<div class="flex flex-col items-center justify-center py-8 text-center">
		<p class="text-destructive mb-4">{data.loadError}</p>
		<Button variant="outline" onclick={() => window.location.reload()}>
			Try Again
		</Button>
	</div>
{:else if data.tour}
	<Button
		variant="ghost"
		class="mb-6 gap-2 text-foreground"
		onclick={handleBack}
	>
		<ArrowLeft class="h-5 w-5 text-foreground" />
		Back
	</Button>

	<div class="space-y-3 mb-4">
		<h1 class="text-2xl font-bold text-foreground">{data.tour.name}</h1>
		<Badge variant="secondary" class="text-sm font-medium">
			{data.tour.theme}
		</Badge>
		<p class="text-muted-foreground">
			{data.tour.description}
		</p>
	</div>

	<Separator class="my-4" />

	<TourView tour={data.tour} />
{:else}
	<div class="flex flex-col items-center justify-center py-8 text-center">
		<p class="text-muted-foreground">Tour not found</p>
		<Button variant="outline" class="mt-4" onclick={handleBack}>
			Return to Tours
		</Button>
	</div>
{/if}