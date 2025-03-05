<!-- src/lib/components/tour/TourView.svelte -->
<script lang="ts">
	import {
		Carousel,
		CarouselContent,
		CarouselItem
	} from "$lib/components/ui/carousel";
	import { Progress } from "$lib/components/ui/progress";
	import { Card, CardContent } from "$lib/components/ui/card";
	import { AspectRatio } from "$lib/components/ui/aspect-ratio";
	import type { Tour } from '$lib/types/tour';
	import type { CarouselAPI } from '$lib/components/ui/carousel/context';
	import { Alert, AlertDescription, AlertTitle } from '$lib/components/ui/alert';
	import { MapPin } from 'lucide-svelte';

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

	function scrollToTop() {
		window.scrollTo({ top: 0, behavior: 'smooth' });
	}

	$effect(() => {
		if (state.api) {
			state.api.on('select', () => {
				if (state.api) {
					const newStop = state.api.selectedScrollSnap();
					if (newStop !== state.currentStop) {
						scrollToTop();
					}
					state.currentStop = newStop;
				}
			});
		}
	});

	$effect(() => {
		state.progress = ((state.currentStop + 1) / tour.stops.length) * 100;
	});
</script>

<div class="w-full max-w-4xl mx-auto space-y-4 overflow-hidden">
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
      watchDrag: false,
      dragFree: false,
      containScroll: "trimSnaps",
    }}
		class="w-full relative"
		setApi={handleApiChange}
	>
		<CarouselContent>
			{#each tour.stops as stop, index (stop.id)}
				<CarouselItem class="basis-full will-change-transform transition-transform">
					<div class="px-4 sm:px-6 md:px-8 h-full">
						<Card class="mx-auto max-w-3xl">
							<AspectRatio ratio={16/9} class="bg-muted">
								<img
									src={stop.artwork.imageUrl}
									alt={stop.artwork.title}
									class="object-contain w-full h-full"
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
										<p class="text-lg text-muted-foreground">
											{#if stop.artwork.fullAttribution}
												{stop.artwork.fullAttribution}
											{:else}
												Unknown Artist
											{/if}
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

								<!-- Navigation Guidance -->
								{#if index < tour.stops.length - 1 && stop.artwork.galleryNumber && tour.stops[index + 1].artwork.galleryNumber && stop.artwork.galleryNumber !== tour.stops[index + 1].artwork.galleryNumber}
									<div class="mt-4">
										<Alert>
											<MapPin class="h-4 w-4 mr-2" />
											<AlertTitle>Next Stop: Gallery {tour.stops[index + 1].artwork.galleryNumber}</AlertTitle>
											<AlertDescription>
												{#if stop.artwork.department !== tour.stops[index + 1].artwork.department && tour.stops[index + 1].artwork.department}
													Head to the {tour.stops[index + 1].artwork.department} section
												{:else}
													{#if parseInt(stop.artwork.galleryNumber) < parseInt(tour.stops[index + 1].artwork.galleryNumber)}
														Follow gallery numbers upward to {tour.stops[index + 1].artwork.galleryNumber}
													{:else}
														Follow gallery numbers downward to {tour.stops[index + 1].artwork.galleryNumber}
													{/if}
												{/if}
											</AlertDescription>
										</Alert>
									</div>
								{/if}
							</CardContent>
						</Card>
					</div>
				</CarouselItem>
			{/each}
		</CarouselContent>
	</Carousel>

	<div class="flex justify-center gap-4 mt-4">
		<button
			class="p-2 rounded-full bg-background/80 backdrop-blur-sm shadow-sm disabled:opacity-50"
			disabled={state.currentStop === 0}
			onclick={() => {
      state.api?.scrollPrev();
      scrollToTop();
    }}
			aria-label="Previous slide"
		>
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
		</button>

		<button
			class="p-2 rounded-full bg-background/80 backdrop-blur-sm shadow-sm disabled:opacity-50"
			disabled={state.currentStop === tour.stops.length - 1}
			onclick={() => {
      state.api?.scrollNext();
      scrollToTop();
    }}
			aria-label="Next slide"
		>
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
		</button>
	</div>
</div>