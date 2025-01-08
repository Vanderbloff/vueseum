<!-- src/lib/components/homepage/artwork/ArtworkFilters.svelte -->
<script lang="ts">
	import { Input } from "$lib/components/ui/input";
	import { Label } from "$lib/components/ui/label";
	import { Checkbox } from "$lib/components/ui/checkbox";
	import { Button } from "$lib/components/ui/button";
	import { Search } from "lucide-svelte";
	import {
		Select,
		SelectContent,
		SelectItem,
		SelectTrigger,
	} from "$lib/components/ui/select";
	import type { ArtworkSort, StandardPeriod } from '$lib/types/artwork';

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
		searchField: 'all' | 'title' | 'artist' | 'medium';
		objectType: string[];
		culturalRegion: string[];
		era: StandardPeriod[];
		department: string[];
		onDisplay: boolean;
		hasImage: boolean;
		sort?: ArtworkSort;
	}

	let { onSearch, children } = $props<{
		onSearch: (filters: ArtworkFilters) => void;
		children?: unknown;
	}>();

	const state = $state({
		filters: {
			searchTerm: [] as string[],
			searchField: "all" as "all" | "title" | "artist" | "medium",
			objectType: [] as string[],
			culturalRegion: [] as string[],
			era: [] as StandardPeriod[],
			department: [] as string[],
			onDisplay: false,
			hasImage: false
		} as ArtworkFilters
	});

	function handleSearchFieldChange(value: string) {
		if (value === "all" || value === "title" || value === "artist" || value === "medium") {
			state.filters.searchField = value;
		}
	}

	function handleObjectTypeChange(value: string[]) {
		state.filters.objectType = value.length > 0 ? value : [];
	}

	function handleCulturalRegionChange(value: string[]) {
		state.filters.culturalRegion = value.length > 0 ? value : [];
	}

	function handleEraChange(value: string | undefined) {
		state.filters.era = value ? [value as StandardPeriod] : [];
	}

	function handleDepartmentChange(value: string[]) {
		state.filters.department = value.length > 0 ? value : [];
	}

</script>

<div class="space-y-6">
	<!-- Main Search Bar -->
	<div class="flex gap-2">
		<Select
			type="single"
			value={state.filters.searchField}
			onValueChange={handleSearchFieldChange}
		>
			<SelectTrigger class="w-36">
                <span>{state.filters.searchField === 'all' ? 'All Fields' :
									state.filters.searchField === 'title' ? 'Title' : 'Artist'}</span>
			</SelectTrigger>
			<SelectContent>
				<SelectItem value="all">All Fields</SelectItem>
				<SelectItem value="title">Title</SelectItem>
				<SelectItem value="artist">Artist</SelectItem>
			</SelectContent>
		</Select>

		<div class="flex-1 relative">
			<Input
				type="text"
				placeholder="Search artworks..."
				bind:value={state.filters.searchTerm[0]}
			/>
			<Button
				variant="ghost"
				size="icon"
				class="absolute right-2 top-1/2 -translate-y-1/2"
				onclick={() => {
        // Ensure non-empty search term is in array format
						const searchTerms = state.filters.searchTerm[0]?.trim()
								? [state.filters.searchTerm[0]]
								: [];
						onSearch({
								...state.filters,
								searchTerm: searchTerms
						});
				}}
			>
				<Search class="h-4 w-4" />
			</Button>
		</div>
	</div>

	{#if children}
		{@render children()}
	{/if}

	<!-- Filter Section -->
	<div>
		<h3 class="text-lg font-semibold mb-3">Filter By</h3>
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
			<div class="space-y-2 min-w-[200px]">
				<div class="flex items-center justify-between h-6">
					<Label for="objectType">Object Type</Label>
					<div class="w-12">
						{#if state.filters.objectType.length > 0}
							<Button
								variant="ghost"
								size="sm"
								onclick={() => handleObjectTypeChange([])}
							>
								Reset
							</Button>
						{/if}
					</div>
				</div>
				<Select
					type="multiple"
					value={state.filters.objectType ? state.filters.objectType : []}
					onValueChange={handleObjectTypeChange}
				>
					<SelectTrigger id="objectType">
						{#if state.filters.objectType.length === 0}
							<span class="text-muted-foreground">Medium / Object type</span>
						{:else}
							<div class="flex items-center gap-1 truncate">
								<!-- Show first 2 selections with comma separation -->
								<span class="truncate">
                {state.filters.objectType.slice(0, 2).join(', ')}
									{#if state.filters.objectType.length > 2}
                    <span class="text-muted-foreground">+{state.filters.objectType.length - 2} more</span>
                {/if}
            </span>
							</div>
						{/if}
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="Painting">Painting</SelectItem>
						<SelectItem value="Sculpture">Sculpture</SelectItem>
						<SelectItem value="Photograph">Photograph</SelectItem>
						<SelectItem value="Silver">Silver</SelectItem>
					</SelectContent>
				</Select>
			</div>

			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label for="culturalRegion">Cultural Region</Label>
					<div class="w-12">
						{#if state.filters.culturalRegion.length > 0}
							<Button
								variant="ghost"
								size="sm"
								onclick={() => handleCulturalRegionChange([])}
							>
								Reset
							</Button>
						{/if}
					</div>
				</div>
				<Select
					type="multiple"
					value={state.filters.culturalRegion ? state.filters.culturalRegion : []}
					onValueChange={handleCulturalRegionChange}
				>
					<SelectTrigger id="culturalRegion">
						{#if state.filters.culturalRegion.length === 0}
							<span class="text-muted-foreground">Cultural region</span>
						{:else}
							<div class="flex items-center gap-1 truncate">
								<!-- Show first 2 selections with comma separation -->
								<span class="truncate">
                {state.filters.culturalRegion.slice(0, 2).join(', ')}
									{#if state.filters.culturalRegion.length > 2}
                    <span class="text-muted-foreground">+{state.filters.culturalRegion.length - 2} more</span>
                {/if}
            </span>
							</div>
						{/if}
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="Europe">Europe</SelectItem>
						<SelectItem value="Asia">Asia</SelectItem>
						<SelectItem value="Americas">Americas</SelectItem>
					</SelectContent>
				</Select>
			</div>

			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label for="era">Era</Label>
					<div class="w-12">
						{#if state.filters.era.length > 0}
							<Button
								variant="ghost"
								size="sm"
								onclick={() => handleEraChange(undefined)}
							>
								Reset
							</Button>
						{/if}
					</div>
				</div>
				<Select
					type="single"
					value={state.filters.era?.length ? state.filters.era[0] : ''}
					onValueChange={handleEraChange}
				>
					<SelectTrigger id="era">
						{#if !state.filters.era.length}
							<span class="text-muted-foreground">Time period</span>
						{:else}
							<span>{state.filters.era[0]}</span>
						{/if}
					</SelectTrigger>
					<SelectContent>
						{#each PERIOD_OPTIONS as period}
							<SelectItem value={period}>{period}</SelectItem>
						{/each}
					</SelectContent>
				</Select>
			</div>

			<div class="space-y-2">
				<div class="flex items-center justify-between h-6">
					<Label for="department">Department</Label>
					<div class="w-12">
						{#if state.filters.department.length > 0}
							<Button
								variant="ghost"
								size="sm"
								onclick={() => handleDepartmentChange([])}
							>
								Reset
							</Button>
						{/if}
					</div>
				</div>
				<Select
					type="multiple"
					value={state.filters.department ? state.filters.department : []}
					onValueChange={handleDepartmentChange}
				>
					<SelectTrigger id="department">
						{#if state.filters.department.length === 0}
							<span class="text-muted-foreground">Museum department</span>
						{:else}
							<div class="flex items-center gap-1 truncate">
								<!-- Show first 2 selections with comma separation -->
								<span class="truncate">
                {state.filters.department.slice(0, 2).join(', ')}
									{#if state.filters.department.length > 2}
                    <span class="text-muted-foreground">+{state.filters.department.length - 2} more</span>
                {/if}
            </span>
							</div>
						{/if}
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="Paintings">Paintings</SelectItem>
						<SelectItem value="Sculpture">Sculpture</SelectItem>
						<SelectItem value="Photographs">Photographs</SelectItem>
					</SelectContent>
				</Select>
			</div>
		</div>
	</div>

	<!-- Checkbox Filters -->
	<div class="flex flex-wrap gap-6">
		<div class="flex items-center space-x-2">
			<Checkbox
				id="onDisplay"
				checked={state.filters.onDisplay}
				onCheckedChange={(checked) => state.filters.onDisplay = checked}
			/>
			<Label for="onDisplay" class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
				On view
			</Label>
		</div>

		<div class="flex items-center space-x-2">
			<Checkbox
				id="hasImage"
				checked={state.filters.hasImage}
				onCheckedChange={(checked) => state.filters.hasImage = checked}
			/>
			<Label for="hasImage" class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
				Has image
			</Label>
		</div>
	</div>
</div>