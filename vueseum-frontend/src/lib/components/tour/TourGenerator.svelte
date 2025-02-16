<script lang="ts">
	import {
		Dialog,
		DialogContent,
		DialogDescription,
		DialogHeader,
		DialogTitle,
		DialogTrigger
	} from '$lib/components/ui/dialog';
	import { Select, SelectContent, SelectItem, SelectTrigger } from '$lib/components/ui/select';
	import { Button } from '$lib/components/ui/button';
	import { Label } from '$lib/components/ui/label';
	import { RadioGroup, RadioGroupItem } from '$lib/components/ui/radio-group';
	import { ScrollArea } from '$lib/components/ui/scroll-area';
	import { Slider } from '$lib/components/ui/slider';
	import { Tooltip, TooltipContent, TooltipTrigger } from '$lib/components/ui/tooltip';
	import {
		AlertDialog,
		AlertDialogAction,
		AlertDialogContent,
		AlertDialogDescription,
		AlertDialogFooter,
		AlertDialogHeader,
		AlertDialogTitle
	} from '$lib/components/ui/alert-dialog';

	import type { TourInputState, StandardPeriod } from '$lib/types/tour-preferences';
	import PreferenceInput from '$lib/components/shared/PreferenceInput.svelte';
	import { tourApi } from '$lib/api/tour';
	import { getOrCreateFingerprint } from '$lib/api/device';
	import { goto } from '$app/navigation';

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
			preferredPeriods: []
		},
		showAdditionalOptions: false,
		generatedToursToday: 0,
		error: null,
		isGenerating: false,
	});

	const canGenerateTour = $derived(state.generatedToursToday < 3);
	const museums = [
		{ id: 1, name: "Metropolitan Museum of Art" }
	];

	function handleMuseumSelect(value: string) {
		if (value !== state.selectedMuseum) {
			state.tourPreferences = {
				theme: "CHRONOLOGICAL",
				numStops: 5,
				preferredArtworks: [],
				preferredArtists: [],
				preferredMediums: [],
				preferredCultures: [],
				preferredPeriods: []
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

		try {
			const visitorId = await getOrCreateFingerprint();
			const preferences = {
				museumId: parseInt(state.selectedMuseum),
				theme: state.tourPreferences.theme,
				maxStops: state.tourPreferences.numStops,
				minStops: Math.max(3, state.tourPreferences.numStops - 2),
				preferredArtworks: artworkInputRef?.getSelections() ?? [],
				preferredArtists: artistInputRef?.getSelections() ?? [],
				preferredMediums: mediumInputRef?.getSelections() ?? [],
				preferredCultures: cultureInputRef?.getSelections() ?? [],
				preferredPeriods: state.tourPreferences.preferredPeriods,
			};

			const newTour = await tourApi.generateTour(visitorId, preferences);

			// Clean up
			artistInputRef?.clearSelections();
			mediumInputRef?.clearSelections();
			cultureInputRef?.clearSelections();
			state.isOpen = false;

			await goto(`/tours/${newTour.id}`);
		} catch (error) {
			if (error instanceof Error) {
				if (error.message === 'TOTAL_LIMIT') {
					state.error = {
						type: 'TOTAL_LIMIT',
						message: 'You\'ve reached the maximum number of tours. Please delete an existing tour before creating a new one.'
					};
				} else if (error.message === 'DAILY_LIMIT') {
					state.error = {
						type: 'DAILY_LIMIT',
						message: 'You\'ve reached your daily tour generation limit. Please try again tomorrow.'
					};
				} else if (error.message === 'INVALID_REQUEST') {
					state.error = {
						type: null,
						message: 'Invalid tour preferences. Please check your selections and try again.'
					};
				} else {
					state.error = {
						type: null,
						message: error.message
					};
				}
			} else {
				state.error = {
					type: null,
					message: 'An unexpected error occurred'
				};
			}
		} finally {
			state.isGenerating = false;
		}
	}
</script>

<div class="text-center py-6">
	<div class="max-w-2xl mx-auto">
		<h2 class="text-2xl font-semibold mb-4">Take Touring Into Your Own Hands</h2>
		<p class="text-muted-foreground mb-6">
			Create a personalized museum tour based on your interests and preferences.
		</p>

		<Dialog bind:open={state.isOpen}>
			{#if canGenerateTour}
				<DialogTrigger>
					<Button size="lg" class="bg-primary text-primary-foreground hover:bg-primary/90">
						Generate My Own Tour
					</Button>
				</DialogTrigger>
			{:else}
				<Tooltip>
					<TooltipTrigger>
						<Button size="lg" disabled class="opacity-50">
							Generate My Own Tour
						</Button>
					</TooltipTrigger>
					<TooltipContent>
						<p>You've reached your tour generation limit for today. Please try again tomorrow!</p>
					</TooltipContent>
				</Tooltip>
			{/if}

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

								{#if state.isGenerating}
									<div class="text-center py-4">
										<p class="text-lg font-medium text-foreground">Your tour is on the way!</p>
										<p class="text-sm text-muted-foreground mt-2">
											We're crafting a personalized experience just for you.
										</p>
									</div>
								{:else}
									<Button
										class="w-full"
										onclick={generateTour}
										disabled={state.isGenerating}
									>
										{state.isGenerating ? 'Generating...' : 'Generate Tour'}
									</Button>
								{/if}
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