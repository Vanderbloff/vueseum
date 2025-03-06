<script lang="ts">
	import {
		Dialog,
		DialogContent,
		DialogDescription,
		DialogHeader,
		DialogTitle
	} from '$lib/components/ui/dialog';
	import { Select, SelectContent, SelectItem, SelectTrigger } from '$lib/components/ui/select';
	import { Button } from '$lib/components/ui/button';
	import { Label } from '$lib/components/ui/label';
	import { RadioGroup, RadioGroupItem } from '$lib/components/ui/radio-group';
	import { ScrollArea } from '$lib/components/ui/scroll-area';
	import { Slider } from '$lib/components/ui/slider';
	import {
		AlertDialog,
		AlertDialogAction,
		AlertDialogContent,
		AlertDialogDescription,
		AlertDialogFooter,
		AlertDialogHeader,
		AlertDialogTitle
	} from '$lib/components/ui/alert-dialog';
	import { Badge } from '$lib/components/ui/badge';

	import type { TourInputState, StandardPeriod, TourPreferences } from '$lib/types/tour-preferences';
	import PreferenceInput from '$lib/components/shared/PreferenceInput.svelte';
	import { tourApi } from '$lib/api/tour';
	import { goto } from '$app/navigation';
	import { Checkbox } from '$lib/components/ui/checkbox';
	import { Alert, AlertDescription } from '$lib/components/ui/alert';
	import { AlertCircle } from 'lucide-svelte';
	import TourGenerationSplash from '$lib/components/tour/TourGenerationSplash.svelte';
	import { onDestroy, onMount } from 'svelte';
	import { toast } from 'svelte-sonner';

	interface PreferenceInputComponent {
		getSelections: () => string[];
		clearSelections: () => void;
	}

	const standardPeriods: StandardPeriod[] = [
		"2000-1000 B.C.",
		"1000 B.C.-A.D. 1",
		"A.D. 1-500",
		"A.D. 500-1000",
		"A.D. 1000-1400",
		"A.D. 1400-1600",
		"A.D. 1600-1800",
		"A.D. 1800-1900",
		"A.D. 1900-present"
	];

	// eslint-disable-next-line svelte/valid-compile
	let artworkInputRef: PreferenceInputComponent = {
		getSelections: () => [],
		clearSelections: () => {}
	};
	// eslint-disable-next-line svelte/valid-compile
	let artistInputRef: PreferenceInputComponent = {
		getSelections: () => [],
		clearSelections: () => {}
	};
	// eslint-disable-next-line svelte/valid-compile
	let mediumInputRef: PreferenceInputComponent = {
		getSelections: () => [],
		clearSelections: () => {}
	};
	// eslint-disable-next-line svelte/valid-compile
	let cultureInputRef: PreferenceInputComponent = {
		getSelections: () => [],
		clearSelections: () => {}
	};

	const state = $state<TourInputState>({
		isOpen: false,
		selectedMuseum: '',
		tourPreferences: {
			theme: "CHRONOLOGICAL",
			numStops: 5,
			preferredArtworks: [],
			preferredArtists: [],
			preferredMediums: [],
			preferredCultures: [],
			preferredPeriods: [],
			preferCloseGalleries: false
		},
		showAdditionalOptions: false,
		generatedToursToday: 0,
		totalToursGenerated: 0,
		error: null,
		isGenerating: false,
		generationStage: null as null | 'selecting' | 'describing' | 'finalizing' | 'complete',
		descriptionProgress: 35,
		currentStop: undefined as number | undefined,
		totalStops: undefined as number | undefined
	});

	const remainingDailyTours = $derived(3 - state.generatedToursToday);
	const remainingTotalTours = $derived(10 - state.totalToursGenerated);
	const remainingTours = $derived(Math.min(remainingDailyTours, remainingTotalTours));
	const canGenerateTour = $derived(remainingTours > 0);
	const isLimitedByDaily = $derived(remainingDailyTours <= remainingTotalTours);

	const museums = [
		{ id: 1, name: "Metropolitan Museum of Art" }
	];

	// Load saved counts from localStorage and synchronize with backend
	onMount(async () => {
		if (typeof window !== 'undefined') {
			// Check if we need to reset daily count
			const lastTourDate = localStorage.getItem('lastTourDate');
			const today = new Date().toDateString();

			if (lastTourDate !== today) {
				// It's a new day, reset daily count
				state.generatedToursToday = 0;
				localStorage.setItem('generatedToursToday', '0');
				localStorage.setItem('lastTourDate', today);
			} else {
				// Load daily count from localStorage
				const storedDailyCount = localStorage.getItem('generatedToursToday');
				if (storedDailyCount) {
					state.generatedToursToday = parseInt(storedDailyCount);
				}
			}

			// Load total count from localStorage as initial value
			const storedTotalCount = localStorage.getItem('totalToursGenerated');
			if (storedTotalCount) {
				state.totalToursGenerated = parseInt(storedTotalCount);
			}
		}

		await synchronizeTourCounts();
	});

	async function synchronizeTourCounts() {
		try {
			// Get the first page with size 1 to get totalElements
			const tourData = await tourApi.getTours(0, 1);

			// Update total tours count to match backend
			state.totalToursGenerated = tourData.totalElements;

			// Save to localStorage
			if (typeof window !== 'undefined') {
				localStorage.setItem('totalToursGenerated', state.totalToursGenerated.toString());
			}
		} catch (error) {
			console.error("Failed to synchronize tour counts:", error);
		}
	}

	function handleDialogOpen() {
		synchronizeTourCounts().then(() => {
			if (!canGenerateTour) {
				// User hit a limit but tried to open dialog anyway
				if (remainingDailyTours <= 0) {
					toast.error("You've reached your daily tour generation limit. Please try again tomorrow.");
				} else {
					toast.error("You've reached the maximum of 10 saved tours.");
				}
				return;
			}

			if (remainingDailyTours === 1 && remainingTotalTours > 1) {
				toast.warning("This is your last tour for today! You can generate more tomorrow.");
			} else if (remainingTotalTours === 1) {
				toast.warning("This is your last tour! You can only save up to 10 tours total.");
			}

			state.isOpen = true;
		});
	}

	function handleMuseumSelect(value: string) {
		if (value !== state.selectedMuseum) {
			state.tourPreferences = {
				theme: "CHRONOLOGICAL",
				numStops: 5,
				preferredArtworks: [],
				preferredArtists: [],
				preferredMediums: [],
				preferredCultures: [],
				preferredPeriods: [],
				preferCloseGalleries: false
			};

			// Clear all PreferenceInput components
			artistInputRef?.clearSelections();
			mediumInputRef?.clearSelections();
			cultureInputRef?.clearSelections();
		}

		state.selectedMuseum = value;
		state.showAdditionalOptions = value.length > 0;
	}

	function handleThemeChange(value: string) {
		if (value === "CHRONOLOGICAL" || value === "ARTIST_FOCUSED" || value === "CULTURAL") {
			state.tourPreferences.theme = value;
		}
	}

	async function generateTour() {
		state.isGenerating = true;
		state.error = null;

		const requestId = crypto.randomUUID();

		state.generationStage = 'selecting';

		state.isOpen = false;

		// Start polling for progress updates
		let pollInterval: number | undefined;
		if (typeof window !== 'undefined') {
			pollInterval = window.setInterval(async () => {
				try {
					const response = await fetch(`/api/v1/tours/progress/${requestId}`);
					if (response.ok) {
						const data = await response.json();

						state.generationStage = data.stage;
						state.descriptionProgress = data.progress;

						if (data.stage === 'describing' && data.currentStop !== undefined) {
							state.currentStop = data.currentStop;
							state.totalStops = data.totalStops;
						}

						if (data.stage === 'complete') {
							if (pollInterval) window.clearInterval(pollInterval);
						}
					}
				} catch (error) {
					console.error("Failed to poll progress", error);
				}
			}, 1000);
		}

		try {
			const rawArtistSelections = artistInputRef?.getSelections() ?? [];

			// Process artist names to strip dates in parentheses
			const processedArtistSelections = rawArtistSelections.map(artist => {
				// Extract just the name part before any parentheses
				return artist.replace(/\s+\(\d{4}-\d{4}\)$/, '');
			});

			console.log('Original artist selections:', rawArtistSelections);
			console.log('Processed artist selections:', processedArtistSelections);

			// Create properly structured preferences object for API
			const preferences: TourPreferences = {
				museumId: parseInt(state.selectedMuseum),
				theme: state.tourPreferences.theme,
				numStops: state.tourPreferences.numStops,
				preferredArtworks: artworkInputRef?.getSelections() ?? [],
				preferredArtists: artistInputRef?.getSelections() ?? [],
				preferredMediums: mediumInputRef?.getSelections() ?? [],
				preferredCultures: cultureInputRef?.getSelections() ?? [],
				preferredPeriods: state.tourPreferences.preferredPeriods,
				preferCloseGalleries: state.tourPreferences.preferCloseGalleries,
				requestId
			};

			const newTour = await tourApi.generateTour(preferences);

			if (pollInterval) window.clearInterval(pollInterval);

			state.generationStage = 'complete';
			state.generatedToursToday++;
			state.totalToursGenerated++;

			if (typeof window !== 'undefined') {
				localStorage.setItem('generatedToursToday', state.generatedToursToday.toString());
				localStorage.setItem('totalToursGenerated', state.totalToursGenerated.toString());
				localStorage.setItem('lastTourDate', new Date().toDateString());
			}

			// Clean up input components
			artistInputRef?.clearSelections();
			mediumInputRef?.clearSelections();
			cultureInputRef?.clearSelections();

			// Navigate to the new tour
			await goto(`/tours/${newTour.id}`);
		} catch (error: unknown) {
			console.error("Tour generation failed:", error);

			// Extract error message safely
			const errorMessage = error instanceof Error ? error.message : String(error);

			if (errorMessage === 'TOTAL_LIMIT') {
				state.error = {
					type: 'TOTAL_LIMIT',
					message: 'You\'ve reached the maximum of 10 saved tours.'
				};

				// Synchronize with backend to get accurate count
				await synchronizeTourCounts();

				toast.error("You can save up to 10 tours total.");
			} else if (errorMessage === 'DAILY_LIMIT') {
				state.error = {
					type: 'DAILY_LIMIT',
					message: 'You\'ve reached the daily limit of 3 tours.'
				};

				toast.error("You can create up to 3 tours per day.");
			} else {
				state.error = {
					type: null,
					message: 'An error occurred while generating your tour.'
				};

				toast.error("Failed to generate tour. Please try again later.");
			}
		} finally {
			if (pollInterval) window.clearInterval(pollInterval);
			state.isGenerating = false;
		}
	}

	onDestroy(() => {
		if (typeof window !== 'undefined') { /* empty */ }
	});
</script>

<div class="text-center py-6">
	<div class="max-w-2xl mx-auto">
		<h2 class="text-2xl font-semibold mb-4">Take Touring Into Your Own Hands</h2>
		<p class="text-muted-foreground mb-6">
			Create a personalized museum tour based on your interests and preferences.
		</p>

		<Dialog bind:open={state.isOpen}>
			<Button
				size="lg"
				class="bg-primary text-primary-foreground hover:bg-primary/90"
				onclick={handleDialogOpen}
				disabled={!canGenerateTour}
			>
				Generate My Own Tour
				<Badge variant={remainingTours <= 1 ? "destructive" : "secondary"} class="ml-2 text-xs">
					{state.generatedToursToday}/3 used today
				</Badge>
			</Button>

			<DialogContent class="sm:max-w-[425px] max-h-[90vh] p-6">
				<DialogHeader class="mb-4">
					<DialogTitle class="text-center text-foreground">Create Your Own Self-Guided Tour</DialogTitle>
					<DialogDescription class="text-center text-foreground">
						Choose your preferences, we'll handle the rest.
					</DialogDescription>
				</DialogHeader>

				<div class="space-y-6 text-foreground">
					<div class="space-y-2">
						<Label for="museum-select">Select a museum</Label>
						<Select
							type="single"
							value={state.selectedMuseum}
							onValueChange={handleMuseumSelect}
						>
							<SelectTrigger>
								{state.selectedMuseum
									? museums.find(m => m.id.toString() === state.selectedMuseum)?.name
									: "Choose a museum"}
							</SelectTrigger>
							<SelectContent>
								{#each museums as museum}
									<SelectItem value={museum.id.toString()}>
										{museum.name}
									</SelectItem>
								{/each}
							</SelectContent>
						</Select>
					</div>

					{#if state.showAdditionalOptions}
						<ScrollArea class="h-[400px] pr-4">
							<div class="space-y-4 px-2">
								<div class="space-y-3">
									<Label>Tour Theme</Label>
									<RadioGroup
										value={state.tourPreferences.theme}
										onValueChange={handleThemeChange}
									>
										<div class="flex items-center space-x-2">
											<RadioGroupItem value="CHRONOLOGICAL" id="chronological" />
											<Label for="chronological">Chronological Journey</Label>
										</div>
										<div class="flex items-center space-x-2">
											<RadioGroupItem value="ARTIST_FOCUSED" id="artist" />
											<Label for="artist">Artist Focus</Label>
										</div>
										<div class="flex items-center space-x-2">
											<RadioGroupItem value="CULTURAL" id="cultural" />
											<Label for="cultural">Cultural Exploration</Label>
										</div>
									</RadioGroup>
								</div>

								<div class="space-y-2">
									<Label>Number of Stops</Label>
									<Slider
										type="single"
										value={state.tourPreferences.numStops}
										onValueChange={(vals) => state.tourPreferences.numStops = vals}
										min={3}
										max={10}
										step={1}
										class="my-4"
									/>
									<p class="text-sm text-muted-foreground">
										Selected stops: {state.tourPreferences.numStops}
									</p>
								</div>

								<PreferenceInput
									bind:this={artworkInputRef}
									label="Preferred Artworks"
									type="ARTWORK"
									placeholder="Search artworks..."
									museumId={state.selectedMuseum}
									currentPreferences={state.tourPreferences}
								/>

								<PreferenceInput
									bind:this={artistInputRef}
									label="Preferred Artists"
									type="ARTIST"
									placeholder="Search artists..."
									museumId={state.selectedMuseum}
									currentPreferences={state.tourPreferences}
								/>

								<PreferenceInput
									bind:this={mediumInputRef}
									label="Preferred Mediums"
									type="MEDIUM"
									placeholder="Search mediums..."
									museumId={state.selectedMuseum}
									currentPreferences={state.tourPreferences}
								/>

								<PreferenceInput
									bind:this={cultureInputRef}
									label="Preferred Cultures"
									type="CULTURE"
									placeholder="Search cultures..."
									museumId={state.selectedMuseum}
									currentPreferences={state.tourPreferences}
								/>

								<div class="space-y-2">
									<Label for="preferred-periods">Preferred Periods</Label>
									<Select
										type="multiple"
										value={state.tourPreferences.preferredPeriods}
										onValueChange={(value) => state.tourPreferences.preferredPeriods = value}
									>
										<SelectTrigger id="preferred-periods">
											<span>
													{state.tourPreferences.preferredPeriods.length > 0
														? `${state.tourPreferences.preferredPeriods.length} selected`
														: "Select one or more time periods to focus your tour"}
											</span>
										</SelectTrigger>
										<SelectContent>
											{#each standardPeriods as period}
												<SelectItem value={period}>
													{period}
												</SelectItem>
											{/each}
										</SelectContent>
									</Select>
								</div>

								<div class="flex items-center space-x-2 mt-4">
									<Checkbox
										id="gallery-proximity-dialog"
										checked={state.tourPreferences.preferCloseGalleries}
										onCheckedChange={(checked) => state.tourPreferences.preferCloseGalleries = checked}
									/>
									<Label for="gallery-proximity-dialog" class="text-sm">
										Prioritize walking convenience
									</Label>
								</div>
								{#if !state.tourPreferences.preferCloseGalleries}
									<Alert variant="default" class="mt-2">
										<AlertCircle class="h-4 w-4" />
										<AlertDescription>
											When selected, tour stops will be organized in sequential gallery order for easier navigation.
											When unselected, the narrative flow will be prioritized, which may scatter stops across the museum.
										</AlertDescription>
									</Alert>
								{/if}

								<Button
									class="w-full"
									onclick={generateTour}
									disabled={state.isGenerating || !canGenerateTour}
								>
									{#if state.isGenerating}
										<div class="flex items-center justify-center">
											<svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
												<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
												<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
											</svg>
											Creating your personalized tour...
										</div>
									{:else if !canGenerateTour}
										{isLimitedByDaily ? "Daily limit reached" : "Tour limit reached"}
									{:else}
										Generate Tour
									{/if}
								</Button>
							</div>
						</ScrollArea>
					{/if}
				</div>
			</DialogContent>
		</Dialog>

		{#if state.error}
			<AlertDialog open={!!state.error}>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>
							{state.error.type === 'TOTAL_LIMIT' ? 'Tour Limit Reached' :
								state.error.type === 'DAILY_LIMIT' ? 'Daily Limit Reached' :
									'Error'}
						</AlertDialogTitle>
						<AlertDialogDescription>
							{state.error.message}
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogAction
							onclick={() => {
								if (state.error?.type === 'TOTAL_LIMIT') {
									state.isOpen = false;
								}
								state.error = null;
							}}
						>
							Okay
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		{/if}
	</div>
</div>

{#if state.isGenerating && state.generationStage}
	<TourGenerationSplash
		stage={state.generationStage}
		progress={state.generationStage === 'selecting' ? 25 :
                 state.generationStage === 'describing' ?
                 (state.descriptionProgress || 35) :
                 state.generationStage === 'finalizing' ? 90 :
                 state.generationStage === 'complete' ? 100 : 0}
		currentStop={state.currentStop}
		totalStops={state.totalStops}
	/>
{/if}