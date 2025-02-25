<script lang="ts">
	import type { StandardPeriod } from '$lib/types/artwork';
	import type { FilterOptions } from '$lib/api/artwork';
	import { Select, SelectContent, SelectItem, SelectTrigger } from '$lib/components/ui/select';
	import { Input } from '$lib/components/ui/input';
	import { Button } from '$lib/components/ui/button';
	import Search from 'lucide-svelte/icons/search';
	import { Label } from '$lib/components/ui/label';
	import { Checkbox } from '$lib/components/ui/checkbox';
	import type { ArtworkFilters, FilterChangeHandler } from '$lib/types/filters';

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
		onFilterChange: FilterChangeHandler
		onSearch: (filters: ArtworkFilters) => void;
		children?: unknown;
	}>();

	function handleClassificationChange(classification: string | undefined) {
		const cleanValue = classification?.split(' (')[0];
		onFilterChange('objectType', cleanValue ? [cleanValue] : []);
	}

	function handleCountryChange(country: string | undefined) {
		const cleanValue = country?.split(' (')[0];
		onFilterChange('country', cleanValue ? [cleanValue] : []);
	}

	function handleRegionChange(region: string | undefined) {
		const cleanValue = region?.split(' (')[0];
		onFilterChange('region', cleanValue ? [cleanValue] : []);
	}

	function handleCultureChange(culture: string | undefined) {
		const cleanValue = culture?.split(' (')[0];
		onFilterChange('culture', cleanValue ? [cleanValue] : []);
	}

	function handleEraChange(value: string | undefined) {
		onFilterChange('era', value ? [value as StandardPeriod] : []);
	}

	function handleMaterialsChange(materials: string[]) {
		const cleanMaterials = materials.map(material =>
			material.split(' (')[0]
		);
		onFilterChange('materials', cleanMaterials);
	}

	function handleSearchFieldChange(value: string) {
		if (value === "all" || value === "title" || value === "artist" || value === "culture") {
			onFilterChange('searchField', value);
		}
	}

	function handleSearch() {
		const searchTerms = filters.searchTerm[0]?.trim()
			? [filters.searchTerm[0]]
			: [];
		onFilterChange('searchTerm', searchTerms);
	}

	// Reset functions for individual filter groups
	function resetArtworkTypeFilters() {
		handleClassificationChange(undefined);
		onFilterChange('materials', []);
	}

	function resetLocationFilters() {
		handleCountryChange(undefined);
		handleRegionChange(undefined);
		handleCultureChange(undefined);
	}

	$effect(() => {
		if (filterOptions.geographicLocations && !filterOptions.countries) {
			filterOptions.countries = filterOptions.geographicLocations;
		}
	});

	console.log("Filter options structure:", filterOptions);
</script>

<div class="space-y-6">
	<!-- Search Bar Section -->
	<div class="space-y-4">
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
							onclick={resetArtworkTypeFilters}
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
					<SelectTrigger class="w-full">
    <span class={!filters.objectType[0] ? "text-muted-foreground" : ""}>
      {filters.objectType[0]?.split(' (')[0] || 'Object type'}
    </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
						{#each filterOptions.objectType as type}
							<SelectItem value={type}>
								{type.split(' (')[0]}
								{#if type.includes('(')}
          <span class="text-muted-foreground ml-1">
            {type.match(/\((\d+)\)/)?.[0] || ''}
          </span>
								{/if}
							</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<Select
					type="single"
					value={filters.materials[0]}
					onValueChange={(material) => handleMaterialsChange(material ? [material] : [])}
				>
					<SelectTrigger class="w-full">
    <span class={!filters.materials[0] ? "text-muted-foreground" : ""}>
      {filters.materials[0]?.split(' (')[0] || 'Medium'}
    </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
						{#each filterOptions.materials as material}
							<SelectItem value={material}>{material.split(' (')[0]}</SelectItem>
						{/each}
					</SelectContent>
				</Select>
			</div>

			<!-- Geographic Location Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Geographical Location</Label>
					{#if filters.country.length || filters.region.length || filters.culture.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={resetLocationFilters}
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
					<SelectTrigger class="w-full">
        <span class={!filters.country[0] ? "text-muted-foreground" : ""}>
            {filters.country[0]?.split(' (')[0] || 'Select country'}
        </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
						{#each (filterOptions.countries || filterOptions.geographicLocations || []) as country}
							<SelectItem value={country}>
								{country.split(' (')[0]}
								{#if country.includes('(')}
                    <span class="text-muted-foreground ml-1">
                        {country.match(/\((\d+)\)/)?.[0] || ''}
                    </span>
								{/if}
							</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<Select
					type="single"
					value={filters.region[0]}
					onValueChange={handleRegionChange}
				>
					<SelectTrigger class="w-full">
    <span class={!filters.region[0] ? "text-muted-foreground" : ""}>
      {filters.region[0]?.split(' (')[0] || 'Select region'}
    </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
						{#each filterOptions.regions || [] as region}
							<SelectItem value={region}>
								{region.split(' (')[0]}
								{#if region.includes('(')}
          <span class="text-muted-foreground ml-1">
            {region.match(/\((\d+)\)/)?.[0] || ''}
          </span>
								{/if}
							</SelectItem>
						{/each}
					</SelectContent>
				</Select>

				<Select
					type="single"
					value={filters.culture[0]}
					onValueChange={handleCultureChange}
				>
					<SelectTrigger class="w-full">
    <span class={!filters.culture[0] ? "text-muted-foreground" : ""}>
      {filters.culture[0]?.split(' (')[0] || 'Select culture'}
    </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
						{#each filterOptions.cultures as culture}
							<SelectItem value={culture}>
								{culture.split(' (')[0]}
								{#if culture.includes('(')}
          <span class="text-muted-foreground ml-1">
            {culture.match(/\((\d+)\)/)?.[0] || ''}
          </span>
								{/if}
							</SelectItem>
						{/each}
					</SelectContent>
				</Select>
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
					<SelectTrigger class="w-full">
    <span class={!filters.era[0] ? "text-muted-foreground" : ""}>
      {filters.era[0] || 'Time period'}
    </span>
					</SelectTrigger>
					<SelectContent align="start" side="bottom" class="w-[300px] max-h-[300px]">
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