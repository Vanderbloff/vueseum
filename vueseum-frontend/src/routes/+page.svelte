<script lang="ts">
	import { Tabs, TabsContent, TabsList, TabsTrigger } from '$lib/components/ui/tabs';
	import { Card, CardContent } from "$lib/components/ui/card";
	import type { PageData } from './$types';
	import ArtworkFilters, { type SearchField } from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import type { StandardPeriod } from '$lib/types/artwork';

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isInitialized: false,
		isLoading: false,
		artworksLoading: true,
		error: null as string | null,
		currentFilters: {
			filters: {
				searchTerm: [] as string[],
				searchField: 'all' as SearchField,
				objectType: [] as string[],
				materials: [] as string[],
				country: [] as string[],
				region: [] as string[],
				culture: [] as string[],
				era: [] as StandardPeriod[],
				hasImage: true,
				museumId: [] as string[]
			}
		},
		filterOptions: {
			objectType: [] as string[],
			materials: [] as string[],
			countries: [] as string[],
			regions: [] as string[],
			cultures: [] as string[]
		},
		loading: {
			options: false,
			initialLoad: true
		}
	});

	type Filters = typeof state.currentFilters.filters;

	// Update the handler for filter changes
	function handleFilterChange<K extends keyof Filters>(
		key: K,
		value: Filters[K]
	) {
		console.log('Filter changed:', key, value);
		state.currentFilters.filters[key] = value;
	}

	function handleTabChange(value: string) {
		console.log('Tab changed:', value);
	}

	function handleSearch(filters: typeof state.currentFilters.filters) {
		console.log('Search triggered with filters:', filters);
		// TODO: Implement actual search functionality
	}

	async function loadFilterOptions(criteria: {
		objectType?: string[];
		country?: string[];
		region?: string[];
	} = {}) {
		try {
			state.loading.options = true;
			// Temporarily mock the API call
			await new Promise(resolve => setTimeout(resolve, 500));

			state.filterOptions = {
				objectType: ['Painting', 'Sculpture'],
				materials: ['Oil paint', 'Bronze'],
				countries: ['France', 'Italy'],
				regions: ['Europe', 'Asia'],
				cultures: ['French', 'Italian']
			};
		} catch (error) {
			state.error = 'Failed to load filter options';
			console.error('Error loading filter options:', error);
		} finally {
			state.loading.options = false;
		}
	}

	$effect(() => {
		if (typeof window === 'undefined') return;
		if (data.initialTab !== 'artworks') return;
		if (state.isInitialized) return;

		const initializeFilters = async () => {
			state.loading.initialLoad = true;
			const loadedOptions = new Set<string>();

			const loadOptionsIfNeeded = async (criteria: {
				objectType?: string[];
				country?: string[];
				region?: string[];
			}) => {
				const key = JSON.stringify(criteria);
				if (!loadedOptions.has(key)) {
					loadedOptions.add(key);
					state.loading.options = true;
					try {
						await loadFilterOptions(criteria);
					} finally {
						state.loading.options = false;
					}
				}
			};

			try {
				await loadOptionsIfNeeded({});
				state.isInitialized = true;
			} catch (error) {
				console.error('Error loading initial filters:', error);
				state.error = 'Failed to load initial filter options';
			} finally {
				state.loading.initialLoad = false;
			}
		};

		initializeFilters();
	});
</script>

<main class="container mx-auto p-4">
	<Tabs value={data.initialTab} onValueChange={handleTabChange}>
		<div class="flex justify-center mb-4">
			<TabsList class="grid w-[400px] grid-cols-2">
				<TabsTrigger value="artworks">Search Artworks</TabsTrigger>
				<TabsTrigger value="tours">Tours</TabsTrigger>
			</TabsList>
		</div>

		<TabsContent value="artworks">
			<div class="w-full max-w-4xl mx-auto">
				{#if state.loading.initialLoad}
					<div class="mt-6 text-center">
						<p class="text-sm text-muted-foreground">
							Loading initial artwork data...
						</p>
					</div>
				{:else}
					<Card>
						<CardContent>
							<ArtworkFilters
								filters={state.currentFilters.filters}
								filterOptions={state.filterOptions}
								loading={{ options: state.loading.options }}
								error={state.error}
								onFilterChange={handleFilterChange}
								onSearch={handleSearch}
							/>
						</CardContent>
					</Card>
					<div class="mt-4">
                <pre class="text-sm">
                    Current Filters: {JSON.stringify(state.currentFilters.filters, null, 2)}
                </pre>
					</div>
				{/if}
			</div>
		</TabsContent>

		<TabsContent value="tours">
			<Card>
				<CardContent>
					<h2>Tours Content</h2>
					<p>Debug mode - basic tabs test</p>
				</CardContent>
			</Card>
		</TabsContent>
	</Tabs>
</main>