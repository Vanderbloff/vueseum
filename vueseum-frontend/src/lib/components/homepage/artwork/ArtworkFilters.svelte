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
	import VirtualizedSelect from "$lib/components/ui/virtualized-select/VirtualizedSelect.svelte";
	import { standardizeFilterOptions } from '$lib/utils/filterStandardization';

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

	// Process filter options efficiently with memoization
	const state = $state({
		categoryOptions: [] as Array<{value: string, label: string, count: number}>,
		originOptions: [] as Array<{value: string, label: string, count: number}>
	});

	let categoryOptionsLoaded = false;
	let originOptionsLoaded = false;

	// Process category options once and cache the result
	$effect(() => {
		if (!categoryOptionsLoaded && filterOptions.objectType?.length) {
			state.categoryOptions = standardizeFilterOptions([
				...(filterOptions.objectType || []),
				...(filterOptions.materials || [])
			]);
			categoryOptionsLoaded = true;
		}
	});

	// Process origin options once and cache the result
	$effect(() => {
		if (!originOptionsLoaded && filterOptions.cultures?.length) {
			state.originOptions = standardizeFilterOptions([
				...(filterOptions.cultures || []),
				...(filterOptions.geographicLocations || []),
				...(filterOptions.regions || [])
			]);
			originOptionsLoaded = true;
		}
	});

	// Shared processing function
	/*function processFilterOptions(options: string[]) {
		// Clean options by removing count information
		const cleanOptions = options.map(opt => opt.split(' (')[0]);

		// Remove duplicates while preserving order of first occurrence
		const uniqueOptions = [...new Set(cleanOptions)];

		// Sort alphabetically
		uniqueOptions.sort((a, b) => a.localeCompare(b));

		// Re-add count information and format for virtualized select
		return uniqueOptions.map(cleanOption => {
			// Find matching options and total their counts
			const matchingOptions = options.filter(opt =>
				opt.split(' (')[0] === cleanOption
			);

			if (matchingOptions.length === 0) {
				return { value: cleanOption, label: cleanOption, count: 0 };
			}

			// Extract and sum counts
			const counts = matchingOptions
				.map(opt => {
					const match = opt.match(/\((\d+)\)/);
					return match ? parseInt(match[1]) : 0;
				});

			const totalCount = counts.reduce((sum, count) => sum + count, 0);

			return {
				value: cleanOption,
				label: cleanOption,
				count: totalCount
			};
		});
	}*/

	function handleEraChange(value: string | undefined) {
		onFilterChange('era', value ? [value as StandardPeriod] : []);
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

	function handleCategoryChange(value: string | undefined) {
		const cleanValue = value?.split(' (')[0];
		onFilterChange('category', cleanValue ? [cleanValue] : []);
	}


	function handleOriginChange(value: string | undefined) {
		const cleanValue = value?.split(' (')[0];
		onFilterChange('origin', cleanValue ? [cleanValue] : []);
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
			<!-- Category Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Category</Label>
					{#if filters.category.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => handleCategoryChange(undefined)}
						>
							Reset
						</Button>
					{/if}
				</div>

				<VirtualizedSelect
					value={filters.category[0]}
					placeholder="Select category"
					items={state.categoryOptions}
					onChange={handleCategoryChange}
					label="Categories"
					loading={loading.options}
				/>
			</div>

			<!-- Origin Filter -->
			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label>Origin</Label>
					{#if filters.origin.length}
						<Button
							variant="ghost"
							size="sm"
							onclick={() => handleOriginChange(undefined)}
						>
							Reset
						</Button>
					{/if}
				</div>

				<VirtualizedSelect
					value={filters.origin[0]}
					placeholder="Select origin"
					items={state.originOptions}
					onChange={handleOriginChange}
					label="Origins"
					loading={loading.options}
				/>
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