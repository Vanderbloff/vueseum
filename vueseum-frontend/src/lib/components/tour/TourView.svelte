<!-- src/lib/components/tour/TourView.svelte -->
<script lang="ts">
	import {
		Carousel,
		CarouselContent,
		CarouselItem,
		CarouselNext,
		CarouselPrevious
	} from "$lib/components/ui/carousel";
	import { Progress } from "$lib/components/ui/progress";
	import { Card, CardContent } from "$lib/components/ui/card";
	import { AspectRatio } from "$lib/components/ui/aspect-ratio";
	import type { Tour } from '$lib/types/tour';
	import type { CarouselAPI } from '$lib/components/ui/carousel/context';

	let { tour } = $props<{
		tour: Tour,
	}>();

	// Note: Carousel transition performance may vary based on browser settings and hardware.
	// Key factors include hardware acceleration and available GPU resources.
	const state = $state({
		currentStop: 0,
		progress: 0,
		api: undefined as CarouselAPI | undefined,
	});

	function handleApiChange(newApi: CarouselAPI | undefined) {
		state.api = newApi;
	}

	$effect(() => {
		if (state.api) {
			state.api.on('select', () => {
				if (state.api) {
					state.currentStop = state.api.selectedScrollSnap();
				}
			});
		}
	});

	// Calculate progress based on currentStop changes
	$effect(() => {
		state.progress = ((state.currentStop + 1) / tour.stops.length) * 100;
	});

</script>

<div class="w-full max-w-4xl mx-auto space-y-4">
	<!-- Progress section -->
	<div class="space-y-2">
		<div class="flex justify-between text-sm text-gray-600">
			<span>Stop {state.currentStop + 1} of {tour.stops.length}</span>
		</div>
		<Progress value={state.progress} />
	</div>

	<!-- Single-item carousel -->
	<Carousel
		opts={{
        align: "center",
        loop: false,
        watchDrag: false,  // Disable drag watching
        dragFree: false,   // Disable free-form dragging
        containScroll: "trimSnaps",  // Ensure proper snap containment
    }}
		class="w-full"
		setApi={handleApiChange}
	>
		<CarouselContent>
			{#each tour.stops as stop (stop.id)}
				<CarouselItem class="basis-full will-change-transform transition-transform">
					<div class="px-4 sm:px-6 md:px-8 h-full">
						<Card class="mx-auto max-w-3xl">
							<AspectRatio ratio={16/9} class="bg-muted">
								<img
									src={stop.artwork.imageUrl}
									alt={stop.artwork.title}
									class="object-cover w-full h-full"
									loading="eager"
									decoding="sync"
								/>
							</AspectRatio>
							<CardContent class="p-3 sm:p-4 space-y-2">
								<div>
									<h2 class="text-xl sm:text-2xl font-semibold break-words">
										{stop.artwork.title}
									</h2>
									<p class="text-gray-600 text-sm sm:text-base break-words">
										{stop.artwork.artist}
									</p>
								</div>

								{#if stop.artwork.galleryNumber}
									<div class="text-sm text-gray-600">
										<strong>Gallery {stop.artwork.galleryNumber}</strong>
										{#if stop.artwork.department}
											<em class="block mt-1">({stop.artwork.department})</em>
										{/if}
									</div>
								{/if}

								<p class="text-gray-700 text-sm sm:text-base whitespace-pre-wrap">
									{stop.description}
								</p>

								{#if stop.recommendedDuration}
									<div class="text-sm text-gray-500">
										Recommended viewing time: {stop.recommendedDuration} minutes
									</div>
								{/if}
							</CardContent>
						</Card>
					</div>
				</CarouselItem>
			{/each}
		</CarouselContent>
		<div class="absolute inset-y-0 left-0 right-0 flex items-center justify-between z-10">
			<div class="pointer-events-auto">
				<CarouselPrevious />
			</div>
			<div class="pointer-events-auto">
				<CarouselNext />
			</div>
		</div>
	</Carousel>
</div>