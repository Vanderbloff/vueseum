<script lang="ts">
	import {
		Dialog,
		DialogContent,
		DialogDescription,
		DialogHeader,
		DialogTitle,
		DialogTrigger
	} from "$lib/components/ui/dialog";
	import {
		Select,
		SelectContent,
		SelectItem,
		SelectTrigger
	} from "$lib/components/ui/select";
	import { Button } from "$lib/components/ui/button";
	import { Label } from "$lib/components/ui/label";
	import { RadioGroup, RadioGroupItem } from "$lib/components/ui/radio-group";
	import { Input } from '$lib/components/ui/input';
	import { ScrollArea } from "$lib/components/ui/scroll-area";
	import { Slider } from "$lib/components/ui/slider";
	import { Tooltip, TooltipContent, TooltipTrigger } from "$lib/components/ui/tooltip";
	import {
		AlertDialog,
		AlertDialogAction,
		AlertDialogContent,
		AlertDialogDescription,
		AlertDialogFooter,
		AlertDialogHeader,
		AlertDialogTitle
	} from "$lib/components/ui/alert-dialog";

	let { onTourGenerated } = $props<{
		onTourGenerated: () => void;
	}>();

	const state = $state({
		isOpen: false,
		selectedMuseum: '',
		tourPreferences: {
			theme: "CHRONOLOGICAL" as "CHRONOLOGICAL" | "ARTIST_FOCUSED" | "CULTURAL",
			numStops: 5,
			requiredArtworks: [] as string[],
			preferredArtists: [] as string[],
			preferredPeriods: [] as string[],
			preferredMediums: [] as string[],
			preferredCultures: [] as string[]
		},
		showAdditionalOptions: false,
		generatedToursToday: 0,
		error: null as { type: 'DAILY_LIMIT' | 'TOTAL_LIMIT' | null, message: string } | null,
		isGenerating: false
	});

	const canGenerateTour = $derived(state.generatedToursToday < 3);

	// Mock museum data - replace with actual API call
	const museums = [
		{ id: 1, name: "Metropolitan Museum of Art" },
		{ id: 2, name: "Louvre Museum" }
	];

	function handleMuseumSelect(value: string) {
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
			const response = await fetch('/api/v1/tours/generate', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({
					visitorId: 'temp-id', // You might want to generate this
					preferences: {
						museumId: parseInt(state.selectedMuseum),
						theme: state.tourPreferences.theme,
						desiredDuration: state.tourPreferences.numStops * 15,
						maxStops: state.tourPreferences.numStops,
						minStops: Math.max(3, state.tourPreferences.numStops - 2),
					}
				})
			});

			if (!response.ok) {
				if (response.status === 507) {
					state.error = {
						type: 'TOTAL_LIMIT',
						message: 'You\'ve reached the maximum number of tours. Please delete an existing tour before creating a new one.'
					};
				} else if (response.status === 429) {
					state.error = {
						type: 'DAILY_LIMIT',
						message: 'You\'ve reached your daily tour generation limit. Please try again tomorrow.'
					};
				} else {
					const errorData = await response.json();
					throw new Error(errorData.message || 'Failed to generate tour');
				}
				return;
			}

			await response.json();
			onTourGenerated();
			state.isOpen = false;

		} catch (error) {
			state.error = {
				type: null,
				message: error instanceof Error ? error.message : 'An unexpected error occurred'
			};
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
					<Button
						size="lg"
						class="bg-primary text-primary-foreground hover:bg-primary/90"
					>
						Generate My Own Tour
					</Button>
				</DialogTrigger>
			{:else}
				<Tooltip>
					<TooltipTrigger>
						<Button
							size="lg"
							disabled
							class="opacity-50"
						>
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
					<DialogTitle class="text-center">Create Your Own Self-Guided Tour</DialogTitle>
					<DialogDescription class="text-center">
						Choose your preferences, we'll handle the rest.
					</DialogDescription>
				</DialogHeader>

				<div class="space-y-6">
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

								<div class="space-y-2">
									<Label for="required-artworks">Required Artworks</Label>
									<Input
										id="required-artworks"
										placeholder="Enter artwork titles, separated by commas"
									/>
									<p class="text-sm text-muted-foreground">
										These artworks will be included in your tour if available
									</p>
								</div>

								<div class="space-y-2">
									<Label for="preferred-artists">Preferred Artists</Label>
									<Input
										id="preferred-artists"
										placeholder="Enter artist names, separated by commas"
									/>
								</div>

								<div class="space-y-2">
									<Label for="preferred-mediums">Preferred Mediums</Label>
									<Input
										id="preferred-mediums"
										placeholder="e.g., Oil painting, sculpture, photograph"
									/>
								</div>

								<div class="space-y-2">
									<Label for="preferred-cultures">Preferred Cultures</Label>
									<Input
										id="preferred-cultures"
										placeholder="e.g., Italian, French, Japanese"
									/>
								</div>

								<Button
									class="w-full"
									onclick={generateTour}
									disabled={state.isGenerating}
								>
									{state.isGenerating ? 'Generating...' : 'Generate Tour'}
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