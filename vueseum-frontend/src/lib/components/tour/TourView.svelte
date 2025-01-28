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

	$effect(() => {
		state.progress = ((state.currentStop + 1) / tour.stops.length) * 100;
	});
</script>

<div class="w-full max-w-4xl mx-auto space-y-4">
	<!-- Progress section -->
	<div class="space-y-2">
		<div class="flex justify-between text-sm text-muted-foreground">
			<span>Stop {state.currentStop + 1} of {tour.stops.length}</span>
		</div>
		<Progress value={state.progress}
		class="mt-2"/>
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
							<CardContent class="p-4 sm:p-6 space-y-4">
								<div class="space-y-2">
									<h2 class="text-2xl font-semibold break-words text-foreground">
										{stop.artwork.title}
									</h2>
									<div class="space-y-1">
										<p class="text-lg text-foreground">
											{stop.artwork.fullAttribution}
										</p>
										<div class="text-muted-foreground space-y-1">
											{#if stop.artwork.creationDate}
												<p>{stop.artwork.creationDate}</p>
											{/if}
											{#if stop.artwork.medium}
												<p>{stop.artwork.medium}</p>
											{/if}
											<div class="flex items-center gap-2 text-muted-foreground">
												{#if stop.artwork.culture}
													<span>{stop.artwork.culture}</span>
												{/if}
												{#if stop.artwork.country}
													<span>{stop.artwork.culture ? '•' : ''} {stop.artwork.country}</span>
												{/if}
												{#if stop.artwork.region}
													<span>{stop.artwork.country ? '•' : ''} {stop.artwork.region}</span>
												{/if}
											</div>
										</div>
									</div>
								</div>

								{#if stop.artwork.galleryNumber || stop.artwork.department}
									<div class="text-sm space-y-1">
										<div class="flex flex-wrap gap-x-3 gap-y-1">
											{#if stop.artwork.galleryNumber}
												<p>
													<strong>Gallery {stop.artwork.galleryNumber}</strong>
												</p>
											{/if}
											{#if stop.artwork.department}
												<p class="text-muted-foreground">
													{stop.artwork.galleryNumber ? '•' : ''} {stop.artwork.department}
												</p>
											{/if}
										</div>
									</div>
								{/if}

								<div class="space-y-3 border-t border-border pt-4">
									<p class="text-foreground whitespace-pre-wrap">
										{stop.tourContextDescription.replace(/^Stop \d+:\s*/, '')}
									</p>
								</div>
							</CardContent>
						</Card>
					</div>
				</CarouselItem>
			{/each}
		</CarouselContent>
		<div class="absolute inset-y-0 left-0 right-0 flex items-center justify-between z-10">
			<div class="pointer-events-auto text-foreground">
				<CarouselPrevious />
			</div>
			<div class="pointer-events-auto text-foreground">
				<CarouselNext />
			</div>
		</div>
	</Carousel>
</div>