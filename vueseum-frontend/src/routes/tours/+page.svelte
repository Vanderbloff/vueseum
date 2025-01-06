<script lang="ts">
	import type { Tour } from '$lib/types/tour';
	import TourHeader from '$lib/components/tour/TourHeader.svelte';
	import TourList from '$lib/components/tour/TourStopList.svelte';

	export let data: { tourId: string, tour: Tour};

	// Get the referrer URL from document if available, otherwise default to homepage
	const returnUrl = typeof document !== 'undefined'
		? document.referrer || '/'
		: '/';
</script>

<div class="container mx-auto px-4 py-6">
	<TourHeader
		tour={{
            name: data.tour.name,
            estimatedDuration: data.tour.estimatedDuration
        }}
		{returnUrl}
	/>
	<h1 class="text-2xl font-bold mb-6">Available Tours</h1>
	<TourList stops={data.tour.stops} />
</div>