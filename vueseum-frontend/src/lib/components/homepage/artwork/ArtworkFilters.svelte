<script module lang="ts">
	export interface ArtworkFilters {
		searchTerm: string[];
		searchField: SearchField;
		objectType: string[];
		materials: string[];
		country: string[];
		region: string[];
		culture: string[];
		era: StandardPeriod[];
		hasImage: boolean;
		museumId: string[];
	}

	export type SearchField = 'all' | 'title' | 'artist' | 'culture';
</script>

<script lang="ts">
	import type { StandardPeriod } from '$lib/types/artwork';
	import type { FilterOptions } from '$lib/api/artwork';
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

	let {
		filters,
		filterOptions,
		loading,
		error,
		onFilterChange,
		children
	} = $props<{
		filters: ArtworkFilters;
		filterOptions: FilterOptions;
		loading: {
			options: boolean;
		};
		error: string | null;
		onFilterChange: (key: keyof ArtworkFilters, value: string[] | StandardPeriod[] | SearchField | boolean) => void;
		onSearch: (filters: ArtworkFilters) => void;
		children?: unknown;
	}>();

	console.log('Initial filterOptions:', filterOptions);

	// Maintain set of in-flight requests to prevent duplicates
	const state = $state({
		pendingRequests: new Set<string>()
	});

	/**
	 * Loads filter options based on current selection criteria.
	 * Only loads dependent options when parent selections change.
	 */
	async function loadFilterOptions(criteria: {
		objectType?: string;
		country?: string;
		region?: string;
	} = {}) {
		const requestId = JSON.stringify(criteria);

		// Skip if this exact request is already in flight
		if (state.pendingRequests.has(requestId)) {
			return;
		}

		try {
			state.pendingRequests.add(requestId);
			loading.options = true;

			const options = await artworkApi.getFilterOptions(criteria);

			// Update only the options that should change based on criteria
			filterOptions = {
				...filterOptions,
				...(criteria.objectType ? { materials: options.mediums } : {}),
				...(criteria.country ? { regions: options.regions } : {}),
				...(criteria.region ? { cultures: options.cultures } : {}),
				// Update base options only if no criteria
				...(!Object.keys(criteria).length ? {
					objectType: options.objectType,
					geographicLocations: options.geographicLocations
				} : {})
			};
		} catch {
			error = 'Failed to load filter options';
		} finally {
			loading.options = false;
			state.pendingRequests.delete(requestId);
		}
	}

	/**
	 * Handles changes to object type selection.
	 * Updates available materials based on selected type.
	 */
	function handleClassificationChange(classification: string | undefined) {
		onFilterChange('objectType', classification ? [classification] : []);
		if (classification) {
			loadFilterOptions({ objectType: classification });
		}
		// Reset materials when type changes
		onFilterChange('materials', []);
	}

	/**
	 * Handles changes to country selection.
	 * Updates available regions based on selected country.
	 */
	function handleCountryChange(country: string | undefined) {
		onFilterChange('country', country ? [country] : []);
		if (country) {
			loadFilterOptions({ country });
		}
		// Reset dependent fields
		onFilterChange('region', []);
		onFilterChange('culture', []);
	}

	/**
	 * Handles changes to region selection.
	 * Updates available cultures based on selected region.
	 */
	function handleRegionChange(region: string | undefined) {
		onFilterChange('region', region ? [region] : []);
		if (region) {
			loadFilterOptions({
				country: filters.country[0],
				region
			});
		}
		// Reset culture when region changes
		onFilterChange('culture', []);
	}

	/**
	 * Handles changes to culture selection.
	 * No dependent fields to update.
	 */
	function handleCultureChange(culture: string | undefined) {
		onFilterChange('culture', culture ? [culture] : []);
	}

	/**
	 * Handles changes to era selection.
	 * Uses predefined PERIOD_OPTIONS for valid values.
	 */
	function handleEraChange(value: string | undefined) {
		onFilterChange('era', value ? [value as StandardPeriod] : []);
	}

	/**
	 * Handles changes to materials selection.
	 * Supports multiple selection.
	 */
	function handleMaterialsChange(materials: string[]) {
		onFilterChange('materials', materials);
	}

	/**
	 * Handles changes to search field selection.
	 * Validates against allowed search field values.
	 */
	function handleSearchFieldChange(value: string) {
		if (value === "all" || value === "title" || value === "artist" || value === "culture") {
			onFilterChange('searchField', value);
		}
	}

	/**
	 * Handles search term changes.
	 * Trims whitespace and handles empty values.
	 */
	function handleSearch() {
		const searchTerms = filters.searchTerm[0]?.trim()
			? [filters.searchTerm[0]]
			: [];
		onFilterChange('searchTerm', searchTerms);
	}

	$effect(() => {
		console.log('filterOptions changed:', filterOptions);
	});
</script>

<div class="space-y-6">
	<!-- Search Bar Section -->
	<div class="space-y-4">
		<!-- Search Field and Button -->
		<div class="flex gap-2">
			<Select
				type="single"
				value={filters.searchField}
				onValueChange={handleSearchFieldChange}
			>
				<SelectTrigger class="w-36">
					{#if filters.searchField === 'all'}
						All Fields
					{:else if filters.searchField === 'title'}
						Title
					{:else if filters.searchField === 'artist'}
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
					value={filters.searchTerm[0] || ''}
					onchange={(e) => onFilterChange('searchTerm', [e.currentTarget.value])}
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
					{#if filters.objectType.length || filters.materials.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => handleClassificationChange(undefined)}
						>
							Reset
						</Button>
					{/if}
				</div>

				<Select
					type="single"
					value={filters.objectType[0]}
					onValueChange={handleClassificationChange}
				>
					<SelectTrigger class="max-w-[300px]">
						<span class={!filters.objectType[0] ? "text-muted-foreground" : ""}>
            {filters.objectType[0] || 'Object type'}
        </span>
					</SelectTrigger>
					<SelectContent>
						{#each filterOptions.objectType as type}
							<SelectItem value={type}>{type}</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<!-- Materials selection appears when classification is selected -->
				{#if filters.objectType.length > 0 && filterOptions.materials.length > 0}
					<Select
						type="multiple"
						value={filters.materials}
						onValueChange={handleMaterialsChange}
					>
						<SelectTrigger class="text-left">
							<div class="flex-1 min-w-0">
								<span id="materials-content" class="truncate block text">
										{filters.materials.length === 0
											? 'Medium'
											: filters.materials.join(', ')}
								</span>
							</div>
						</SelectTrigger>
						<SelectContent>
							{#each filterOptions.materials as material}
								<SelectItem value={material}>{material}</SelectItem>
							{/each}
						</SelectContent>
					</Select>
				{/if}
			</div>

			<!-- Geographic Location Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Geographical Location</Label>
					{#if filters.country.length || filters.region.length || filters.culture.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => handleCountryChange(undefined)}
						>
							Reset
						</Button>
					{/if}
				</div>

				<!-- Country Selection -->
				<Select
					type="single"
					value={filters.country[0]}
					onValueChange={handleCountryChange}
				>
					<SelectTrigger>
						<span class={!filters.country[0] ? "text-muted-foreground" : ""}>
								{filters.country[0] || 'Select country'}
						</span>
					</SelectTrigger>
					<SelectContent>
						{#each filterOptions.geographicLocations as country}
							<SelectItem value={country}>{country}</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<!-- Region Selection - Only show if country is selected -->
				{#if filters.country.length > 0 && filterOptions.regions.length > 0}
					<Select
						type="single"
						value={filters.region[0]}
						onValueChange={handleRegionChange}
					>
						<SelectTrigger>
							<span class={!filters.region[0] ? "text-muted-foreground" : ""}>
									{filters.region[0] || 'Select region'}
							</span>
						</SelectTrigger>
						<SelectContent>
							{#each filterOptions.regions as region}
								<SelectItem value={region}>{region}</SelectItem>
							{/each}
						</SelectContent>
					</Select>
				{/if}

				<!-- Culture Selection - Show based on selected region -->
				{#if filters.region.length > 0 && filterOptions.cultures.length > 0}
					<Select
						type="single"
						value={filters.culture[0]}
						onValueChange={handleCultureChange}
					>
						<SelectTrigger>
							<span class={!filters.culture[0] ? "text-muted-foreground" : ""}>
									{filters.culture[0] || 'Select culture'}
							</span>
						</SelectTrigger>
						<SelectContent>
							{#each filterOptions.cultures as culture}
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
					{#if filters.era.length}
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
					value={filters.era[0]}
					onValueChange={handleEraChange}
				>
					<SelectTrigger>
						<span class={!filters.era[0] ? "text-muted-foreground" : ""}>
								{filters.era[0] || 'Time period'}
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
					id="hasImage"
					checked={filters.hasImage}
					onCheckedChange={(checked) => onFilterChange('hasImage', checked)}
				/>
				<Label for="hasImage">Has image</Label>
			</div>
		</div>
	</div>

	{#if loading.options}
		<div class="flex justify-center py-4">
			<span class="text-sm text-muted-foreground">Loading filter options...</span>
		</div>
	{/if}

	<!-- Error Display -->
	{#if error}
		<div class="text-sm text-destructive mt-2">
			{error}
		</div>
	{/if}
</div>