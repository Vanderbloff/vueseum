<!-- src/routes/tours/[id]/+page.svelte -->
<script lang="ts">
	import { Badge } from "$lib/components/ui/badge";
	import { Separator } from "$lib/components/ui/separator";
	import TourView from '$lib/components/tour/TourView.svelte';
	import { Button } from "$lib/components/ui/button";
	import { goto } from '$app/navigation';
	import { ArrowLeft } from 'lucide-svelte';

	let { data } = $props();
	let returnUrl = '/?tab=tours';

	function handleBack() {
		goto(returnUrl);
	}
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