
<script lang="ts">
	import type { StandardPeriod } from '$lib/types/artwork';
	import { artworkApi } from '$lib/api/artwork';
	import { Select, SelectContent, SelectItem, SelectTrigger } from '$lib/components/ui/select';
	import { Input } from '$lib/components/ui/input';
	import { Button } from '$lib/components/ui/button';
	import Search from 'lucide-svelte/icons/search';
	import { Label } from '$lib/components/ui/label';
	import { Checkbox } from '$lib/components/ui/checkbox';

	const PERIOD_OPTIONS: StandardPeriod[] = [
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

	export interface ArtworkFilters {
		searchTerm: string[];
		searchField: SearchField;
		objectType: string[];
		materials: string[];
		country: string[];
		region: string[];
		culture: string[];
		era: StandardPeriod[];
		onDisplay: boolean;
		hasImage: boolean;
	}

	export type SearchField = 'all' | 'title' | 'artist' | 'culture';

	let { onSearch, children } = $props<{
		onSearch: (filters: ArtworkFilters) => void;
		children?: unknown;
	}>();

	const state = $state({
		filters: {
			searchTerm: [] as string[],
			searchField: "all" as SearchField,
			objectType: [] as string[],
			materials: [] as string[],
			culture: [] as string[],
			country: [] as string[],
			region: [] as string[],
			era: [] as StandardPeriod[],
			onDisplay: false,
			hasImage: true
		},
		filterOptions: {
			objectType: [] as string[],
			materials: [] as string[],
			countries: [] as string[],
			regions: [] as string[],
			cultures: [] as string[]
		},
		loading: {
			options: false
		},
		error: null as string | null,
		isTruncated: false,
		materialsTrigger: null as HTMLElement | null
	});

	// Fetch initial filter options
	async function fetchFilterOptions(): Promise<void> {
		if (state.loading.options) return;

		state.loading.options = true;
		state.error = null;

		try {
			state.filterOptions = await artworkApi.getFilterOptions({});
		} catch (error) {
			state.error = error instanceof Error ? error.message : 'Failed to load filter options';
			console.error('Error fetching filter options:', error);
		} finally {
			state.loading.options = false;
		}
	}

	// Handle search field changes
	function handleSearchFieldChange(value: string) {
		if (value === "all" || value === "title" || value === "artist" || value === "culture") {
			state.filters.searchField = value;
		}
	}

	// Handle object type selection and load subtypes
	async function handleClassificationChange(classification: string | undefined) {
		// Update classification filter
		state.filters.objectType = classification ? [classification] : [];

		// Reset materials when classification changes
		state.filters.materials = [];

		if (classification) {
			state.loading.options = true;
			try {
				// Fetch materials relevant to this classification
				const response = await artworkApi.getFilterOptions({
					artworkType: classification
				});
				state.filterOptions.materials = response.materials || [];
			} catch (error) {
				state.error = error instanceof Error ?
					error.message : 'Failed to load options';
			} finally {
				state.loading.options = false;
			}
		}

		// Trigger search with updated filters
		onSearch(state.filters);
	}

	// Handle materials selection
	function handleMaterialsChange(materials: string[]) {
		state.filters.materials = materials;
		onSearch(state.filters);
	}

	async function handleCountryChange(country: string | undefined) {
		state.filters.country = country ? [country] : [];
		state.filters.region = [];
		state.filters.culture = [];

		if (country) {
			try {
				const response = await artworkApi.getFilterOptions({ country });
				state.filterOptions.regions = response.regions || [];
			} catch (error) {
				state.error = error instanceof Error ? error.message : 'Failed to load regions';
			}
		}

		onSearch(state.filters);
	}

	// Handle region selection and load cultures
	async function handleRegionChange(region: string | undefined) {
		state.filters.region = region ? [region] : [];
		state.filters.culture = [];

		if (region) {
			try {
				const response = await artworkApi.getFilterOptions({
					country: state.filters.country[0],
					region
				});
				state.filterOptions.cultures = response.cultures || [];
			} catch (error) {
				state.error = error instanceof Error ? error.message : 'Failed to load cultures';
			}
		}

		onSearch(state.filters);
	}

	// Handle culture selection
	function handleCultureChange(culture: string | undefined) {
		state.filters.culture = culture ? [culture] : [];
		onSearch(state.filters);
	}

	// Handle era selection
	function handleEraChange(value: string | undefined) {
		state.filters.era = value ? [value as StandardPeriod] : [];
		onSearch(state.filters);
	}

	// Handle checkbox changes
	function handleCheckboxChange(key: 'onDisplay' | 'hasImage', checked: boolean) {
		state.filters[key] = checked;
		onSearch(state.filters);
	}

	// Handle search execution
	function handleSearch() {
		const searchTerms = state.filters.searchTerm[0]?.trim()
			? [state.filters.searchTerm[0]]
			: [];

		onSearch({
			...state.filters,
			searchTerm: searchTerms
		});
	}
	// Initial load effect
	$effect(() => {
		if (!state.filterOptions.objectType.length && !state.loading.options) {
			fetchFilterOptions();
		}
	});
</script>

<div class="space-y-6">
	<!-- Search Bar Section -->
	<div class="space-y-4">
		<!-- Search Field and Button -->
		<div class="flex gap-2">
			<Select
				type="single"
				value={state.filters.searchField}
				onValueChange={handleSearchFieldChange}
			>
				<SelectTrigger class="w-36">
					{#if state.filters.searchField === 'all'}
						All Fields
					{:else if state.filters.searchField === 'title'}
						Title
					{:else if state.filters.searchField === 'artist'}
						Artist
					{:else}
						Culture
					{/if}
				</SelectTrigger>
				<SelectContent>
					<SelectItem value="all">All Fields</SelectItem>
					<SelectItem value="title">Title</SelectItem>
					<SelectItem value="artist">Artist</SelectItem>
					<SelectItem value="culture">Culture</SelectItem>
				</SelectContent>
			</Select>

			<div class="flex-1 relative">
				<Input
					type="text"
					placeholder="Search artworks..."
					value={state.filters.searchTerm[0] || ''}
					onchange={(e) => state.filters.searchTerm = [e.currentTarget.value]}
					onkeydown={(e) => e.key === 'Enter' && handleSearch()}
				/>
				<Button
					variant="ghost"
					size="icon"
					class="absolute right-2 top-1/2 -translate-y-1/2"
					onclick={handleSearch}
				>
					<Search class="h-4 w-4" />
				</Button>
			</div>
		</div>
	</div>

	{#if children}
		{@render children()}
	{/if}

	<!-- Filter Section -->
	<div>
		<h3 class="text-lg font-semibold mb-3">Filter By</h3>

		<!-- Main Filter Grid -->
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
			<!-- Object Type Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Artwork Type</Label>
					{#if state.filters.objectType.length || state.filters.materials.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => {
								handleClassificationChange(undefined);
						}}
						>
							Reset
						</Button>
					{/if}
				</div>

				<Select
					type="single"
					value={state.filters.objectType[0]}
					onValueChange={handleClassificationChange}
				>
					<SelectTrigger>
						<span class={!state.filters.objectType[0] ? "text-muted-foreground" : ""}>
            	{state.filters.objectType[0] || 'Object type'}
        		</span>
					</SelectTrigger>
					<SelectContent>
						{#each state.filterOptions.objectType || [] as type}
							<SelectItem value={type}>{type}</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<!-- Materials selection appears when classification is selected -->
				{#if state.filters.objectType.length > 0 && state.filterOptions.materials.length > 0}
					<Select
						type="multiple"
						value={state.filters.materials}
						onValueChange={handleMaterialsChange}
					>
						<SelectTrigger class="text-left">
							<div class="flex-1 min-w-0"> <!-- Use flexbox and min-width to prevent overflow -->
								<span id="materials-content" class="truncate block text">
                {state.filters.materials.length === 0
									? 'Medium'
									: state.filters.materials.join(', ')}
            		</span>
							</div>
						</SelectTrigger>
						<SelectContent>
							{#each state.filterOptions.materials as material}
								<SelectItem value={material}>{material}</SelectItem>
							{/each}
						</SelectContent>
					</Select>
				{/if}
			</div>

			<!-- Cultural Region Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Geographical Location</Label>
					{#if state.filters.country.length || state.filters.region.length || state.filters.culture.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => {
                    state.filters.country = [];
                    state.filters.region = [];
                    state.filters.culture = [];
                    onSearch(state.filters);
                }}
						>
							Reset
						</Button>
					{/if}
				</div>

				<!-- Country Selection -->
				<Select
					type="single"
					value={state.filters.country[0]}
					onValueChange={handleCountryChange}
				>
					<SelectTrigger>
            <span class={!state.filters.country[0] ? "text-muted-foreground" : ""}>
                {state.filters.country[0] || 'Select country'}
            </span>
					</SelectTrigger>
					<SelectContent>
						{#each state.filterOptions.countries as country}
							<SelectItem value={country}>{country}</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<!-- Region Selection - Only show if country is selected -->
				{#if state.filters.country.length > 0 && state.filterOptions.regions.length > 0}
					<Select
						type="single"
						value={state.filters.region[0]}
						onValueChange={handleRegionChange}
					>
						<SelectTrigger>
                <span class={!state.filters.region[0] ? "text-muted-foreground" : ""}>
                    {state.filters.region[0] || 'Select region'}
                </span>
						</SelectTrigger>
						<SelectContent>
							{#each state.filterOptions.regions as region}
								<SelectItem value={region}>{region}</SelectItem>
							{/each}
						</SelectContent>
					</Select>
				{/if}

				<!-- Culture Selection - Show based on selected region -->
				{#if state.filters.region.length > 0 && state.filterOptions.cultures.length > 0}
					<Select
						type="single"
						value={state.filters.culture[0]}
						onValueChange={handleCultureChange}
					>
						<SelectTrigger>
                <span class={!state.filters.culture[0] ? "text-muted-foreground" : ""}>
                    {state.filters.culture[0] || 'Select culture'}
                </span>
						</SelectTrigger>
						<SelectContent>
							{#each state.filterOptions.cultures as culture}
								<SelectItem value={culture}>{culture}</SelectItem>
							{/each}
						</SelectContent>
					</Select>
				{/if}
			</div>

			<!-- Era Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Era</Label>
					{#if state.filters.era.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => handleEraChange(undefined)}
						>
							Reset
						</Button>
					{/if}
				</div>

				<Select
					type="single"
					value={state.filters.era[0]}
					onValueChange={handleEraChange}
				>
					<SelectTrigger>
						<span class={!state.filters.era[0] ? "text-muted-foreground" : ""}>
            	{state.filters.era[0] || 'Time period'}
        		</span>
					</SelectTrigger>
					<SelectContent>
						{#each PERIOD_OPTIONS as period}
							<SelectItem value={period}>{period}</SelectItem>
						{/each}
					</SelectContent>
				</Select>
			</div>
		</div>

		<!-- Checkbox Filters -->
		<div class="flex flex-wrap gap-6 mt-6">
			<div class="flex items-center space-x-2">
				<Checkbox
					id="onDisplay"
					checked={state.filters.onDisplay}
					onCheckedChange={(checked) => handleCheckboxChange('onDisplay', checked)}
				/>
				<Label for="onDisplay">On view</Label>
			</div>

			<div class="flex items-center space-x-2">
				<Checkbox
					id="hasImage"
					checked={state.filters.hasImage}
					onCheckedChange={(checked) => handleCheckboxChange('hasImage', checked)}
				/>
				<Label for="hasImage">Has image</Label>
			</div>
		</div>
	</div>

	<!-- Error Display -->
	{#if state.error}
		<div class="text-sm text-destructive mt-2">
			{state.error}
		</div>
	{/if}
</div>