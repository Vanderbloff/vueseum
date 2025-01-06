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

	// State management using Svelte 5 runes
	// Note: selectedMuseum must be a string array to match the Select component's expectations
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
		showAdditionalOptions: false
	});

	// Mock museum data - replace with actual API call
	const museums = [
		{ id: 1, name: "Metropolitan Museum of Art" },
		{ id: 2, name: "Louvre Museum" }
	];

	// Handler updated to accept string array type
	function handleMuseumSelect(value: string) {
		state.selectedMuseum = value;
		state.showAdditionalOptions = value.length > 0;
	}

	function handleThemeChange(value: string) {
		if (value === "CHRONOLOGICAL" || value === "ARTIST_FOCUSED" || value === "CULTURAL") {
			state.tourPreferences.theme = value;
		}
	}

</script>

<div class="text-center py-8">
	<div class="max-w-2xl mx-auto">
		<h2 class="text-2xl font-semibold mb-4">
			Start Your Art Journey
		</h2>
		<p class="text-gray-600 mb-6">
			Create a personalized museum tour based on your interests and preferences.
		</p>

		<Dialog bind:open={state.isOpen}>
			<DialogTrigger>
				<Button
					size="lg"
					class="bg-primary text-primary-foreground hover:bg-primary/90"
				>
					Generate Your Tour
				</Button>
			</DialogTrigger>

			<DialogContent class="sm:max-w-[425px] max-h-[90vh] p-6">
				<DialogHeader class="mb-4">
					<DialogTitle class="text-center">Create Your Self-Guided Tour</DialogTitle>
					<DialogDescription class="text-center">
						Choose your preferences to generate your personalized museum tour.
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
								{state.selectedMuseum[0]
									? museums.find(m => m.id.toString() === state.selectedMuseum[0])?.name
									: "Choose a museum"}
							</SelectTrigger>
							<SelectContent>
								{#each museums as museum}
									<SelectItem
										value={museum.id.toString()}
									>
										{museum.name}
									</SelectItem>
								{/each}
							</SelectContent>
						</Select>
					</div>

					{#if state.showAdditionalOptions}
						<ScrollArea class="h-[400px] pr-4">
							<div class="space-y-4 px-2">
								<!-- Tour Theme Selection -->
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

								<!-- Required Artworks -->
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

								<!-- Preferred Artists -->
								<div class="space-y-2">
									<Label for="preferred-artists">Preferred Artists</Label>
									<Input
										id="preferred-artists"
										placeholder="Enter artist names, separated by commas"
									/>
								</div>

								<!-- Preferred Mediums -->
								<div class="space-y-2">
									<Label for="preferred-mediums">Preferred Mediums</Label>
									<Input
										id="preferred-mediums"
										placeholder="e.g., Oil painting, sculpture, photograph"
									/>
								</div>

								<!-- Preferred Cultures -->
								<div class="space-y-2">
									<Label for="preferred-cultures">Preferred Cultures</Label>
									<Input
										id="preferred-cultures"
										placeholder="e.g., Italian, French, Japanese"
									/>
								</div>

								<!-- Generate Tour Button -->
								<Button class="w-full">
									Generate Tour
								</Button>
							</div>
							</ScrollArea>
						{/if}
					</div>
			</DialogContent>
		</Dialog>
	</div>
</div>