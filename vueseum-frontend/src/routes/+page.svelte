<script lang="ts">
	import { Tabs, TabsContent, TabsList, TabsTrigger } from '$lib/components/ui/tabs';
	import { Card, CardContent } from "$lib/components/ui/card";
	import type { PageData } from './$types';
	import ArtworkFilters, { type SearchField } from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import type { StandardPeriod } from '$lib/types/artwork';

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isLoading: false,
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
				<Card>
					<CardContent>
						<ArtworkFilters
							filters={state.currentFilters.filters}
							filterOptions={state.filterOptions}
							loading={{ options: false }}
							error={null}
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