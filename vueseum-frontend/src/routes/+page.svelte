<script lang="ts">
	import { Tabs, TabsContent, TabsList, TabsTrigger } from '$lib/components/ui/tabs';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import ArtworkCard from '$lib/components/homepage/artwork/ArtworkCard.svelte';
	import ArtworkFilters from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import ArtworkModal from '$lib/components/homepage/artwork/ArtworkModal.svelte';
	import TourList from '$lib/components/tour/TourList.svelte';
	import type { Artwork } from '$lib/types/artwork';
	import { goto } from '$app/navigation';
	import {
		Pagination,
		PaginationContent, PaginationItem,
		PaginationLink,
		PaginationNextButton,
		PaginationPrevButton
	} from '$lib/components/ui/pagination';
	import TourGenerator from '$lib/components/tour/TourGenerator.svelte';
	import ErrorDisplay from '$lib/components/homepage/artwork/ErrorDisplay.svelte';
	import SortControls from '$lib/components/homepage/artwork/SortControls.svelte';
	import GridSkeleton from '$lib/components/shared/GridSkeleton.svelte';
	import { artworkApi } from '$lib/api/artwork';
	import { updateUrlParams } from '$lib/utils/urlParams';
	import { debounce } from '$lib/utils/debounce';
	import type { PageData } from './types';

	let { data } = $props<{
		data: PageData;
	}>();

	const state = $state({
		isInitialized: false,
		selectedArtwork: null as Artwork | null,
		isModalOpen: false,
		artworksLoading: false,
		artworksData: data.artworks,
		currentPage: 1,
		pageSize: 12,
		error: null as {
			type: 'search' | 'load' | 'pagination',
			message: string,
			retryFn?: () => void } | null,
		currentFilters: {
			filters: {
				...data.initialFilters
			},
			sort: {
				field: data.initialSort.field,
				direction: data.initialSort.direction
			}
		},
		filterOptions: data.filterOptions ?? {
			objectType: [] as string[],
			materials: [] as string[],
			countries: [] as string[],
			regions: [] as string[],
			cultures: [] as string[]
		},
		loading: {
			options: false,
			results: false,
			initialLoad: hasUrlParameters()
		},
		pendingRequests: new Set<string>()
	});

	function hasUrlParameters() {
		if (typeof window === 'undefined') return false;
		const url = new URL(window.location.href);
		// Only count filter-related parameters, not tab or page parameters
		const filterParams = ['q', 'searchField', 'objectType', 'medium', 'country',
			'region', 'culture', 'period', 'sortBy', 'sortDirection'];
		return Array.from(url.searchParams.entries())
			.some(([key]) => filterParams.includes(key));
	}

	const debouncedSearch = debounce(async (filters: typeof state.currentFilters.filters) => {
		state.artworksLoading = true;
		state.error = null;

		try {
			const results = await artworkApi.searchArtworks(
				filters,
				0,  // first page
				state.pageSize,
				state.currentFilters.sort.field !== 'relevance' ? {
					field: state.currentFilters.sort.field,
					direction: state.currentFilters.sort.direction
				} : undefined
			);

			if (results.totalElements === 0) {
				state.error = {
					type: 'search',
					message: 'No artworks match your search criteria. Try adjusting your filters.'
				};
				return;
			}
			state.artworksData = results;
		} catch (error) {
			console.error('Error performing search:', error);
			state.error = {
				type: 'search',
				message: 'An error occurred while searching. Please try again.',
				retryFn: () => handleSearch(filters)
			};
		} finally {
			state.artworksLoading = false;
		}
	}, 300);

	async function handleSearch(filters: typeof state.currentFilters.filters) {
		console.log('Performing search with filters:', JSON.stringify(filters));
		debouncedSearch(filters);
	}

	async function loadAllFilterOptions() {
		const requestId = "allOptions";

		// Skip if already loading
		if (state.pendingRequests.has(requestId)) {
			return;
		}

		try {
			state.pendingRequests.add(requestId);
			state.loading.options = true;

			const options = await artworkApi.getFilterOptions({});

			// Update all filter options at once
			state.filterOptions = {
				objectType: options.objectType || [],
				materials: options.materials || [],
				countries: options.geographicLocations || [],
				regions: options.regions || [],
				cultures: options.cultures || []
			};
		} catch (error) {
			console.error('Error loading filter options:', error);
			state.error = {
				type: 'load',
				message: 'Failed to load filter options',
				retryFn: () => loadAllFilterOptions()
			};
		} finally {
			state.loading.options = false;
			state.pendingRequests.delete(requestId);
		}
	}

	function handleFilterChange(key: string, value: any) {
		state.currentFilters.filters[key as keyof ArtworkFilters] = value;
		handleSearch(state.currentFilters.filters);
	}

	async function handlePageChange(newPage: number) {
		try {
			state.artworksLoading = true;
			state.error = null;

			// Get all results if we're sorting
			const results = await artworkApi.searchArtworks(
				state.currentFilters.filters,
				newPage - 1,
				state.pageSize,
				state.currentFilters.sort.field !== 'relevance' ? {
					field: state.currentFilters.sort.field,
					direction: state.currentFilters.sort.direction,
				} : undefined,
			);

			if (!results || !results.content) {
				state.error = {
					type: 'pagination',
					message: 'Invalid response data received. Please try again.',
					retryFn: () => handlePageChange(newPage)
				};
				return;
			}

			if (results.content.length === 0) {
				state.error = {
					type: 'search',
					message: newPage > 1
						? 'This page does not exist. Try returning to the first page.'
						: 'No artworks found matching your criteria. Try adjusting your filters.',
					retryFn: newPage > 1
						? () => handlePageChange(1)
						: undefined
				};
				return;
			}

			state.artworksData = results;
			state.currentPage = newPage;
		} catch {
			state.error = {
				type: 'pagination',
				message: 'An error occurred while loading the page. Please try again.',
				retryFn: () => handlePageChange(newPage)
			};
		} finally {
			state.artworksLoading = false;
		}
	}

	function handleTabChange(value: string) {
		const url = new URL(window.location.href);
		url.searchParams.set('tab', value);
		goto(url.toString(), { replaceState: true });
	}

	function handleArtworkClick(artwork: Artwork) {
		state.selectedArtwork = artwork;
		state.isModalOpen = true;
	}

	function handleCloseModal() {
		state.isModalOpen = false;
		state.selectedArtwork = null;
	}

	const hasExistingTours = $derived(
		(data?.tours?.content?.length ?? 0) > 0
	);

	// URL synchronization - maintains URL state as filters change
	// Enables bookmarking and sharing of search results
	$effect(() => {
		if (!state.isInitialized) return;

		// Don't update URL during SSR
		if (typeof window === 'undefined') return;

		// Only update if we're on the artworks tab
		if (data.initialTab !== 'artworks') return;

		const params = {
			// Search parameters
			q: state.currentFilters.filters.searchTerm[0] || null,
			searchField: state.currentFilters.filters.searchField === 'all' ?
				null : state.currentFilters.filters.searchField,

			// Filter parameters
			objectType: state.currentFilters.filters.objectType,
			medium: state.currentFilters.filters.materials,
			country: state.currentFilters.filters.country,
			region: state.currentFilters.filters.region,
			culture: state.currentFilters.filters.culture,
			period: state.currentFilters.filters.era,
			hasImage: state.currentFilters.filters.hasImage === false ? 'false' : null,

			// Sort parameters
			sortBy: state.currentFilters.sort.field !== 'relevance' ?
				state.currentFilters.sort.field : null,
			sortDirection: state.currentFilters.sort.field !== 'relevance' ?
				state.currentFilters.sort.direction : null,

			// Pagination
			page: state.currentPage > 1 ? state.currentPage.toString() : null
		};

		updateUrlParams(params);
	});

	// Initial state setup and URL-based loading
	// Handles initialization of filters and loading states from URL parameters
	$effect(() => {
		if (typeof window === 'undefined') return;
		if (data.initialTab !== 'artworks') return;
		if (state.isInitialized) return;

		const url = new URL(window.location.href);
		const hasSearchParams = Array.from(url.searchParams.entries()).length > 0;

		// Set initial page if it exists
		const pageParam = url.searchParams.get('page');
		if (pageParam) {
			state.currentPage = Number(pageParam);
		}

		// Check if hasImage is explicitly set to false in URL
		const hasImageParam = url.searchParams.get('hasImage');
		const explicitlyDisabled = hasImageParam === 'false';

		// Set hasImage to true by default unless explicitly disabled
		if (!explicitlyDisabled) {
			state.currentFilters.filters.hasImage = true;
		}

		// Only initialize filters if we have URL parameters
		if (hasSearchParams || !explicitlyDisabled) {
			const initializeFilters = async () => {
				state.loading.initialLoad = true;
				try {
					// Load all filter options at once
					await loadAllFilterOptions();

					// Search with current filters
					state.loading.results = true;
					await handleSearch(state.currentFilters.filters);
				} catch (error) {
					console.error('Error loading initial filters:', error);
					state.error = {
						type: 'load',
						message: 'Failed to load initial filter options',
						retryFn: () => initializeFilters()
					};
				} finally {
					state.loading.initialLoad = false;
					state.loading.results = false;
				}
			};

			initializeFilters();
		} else {
			handleSearch(state.currentFilters.filters);
		}

		state.isInitialized = true;
	});

	// Responsive layout management
	// Adjusts grid layout based on viewport size
	$effect(() => {
		if (typeof window === 'undefined') return;

		function updatePageSize() {
			if (window.innerWidth >= 1024) {
				state.pageSize = 12;     // Desktop
			} else if (window.innerWidth >= 768) {
				state.pageSize = 9;      // Tablet
			} else {
				state.pageSize = 6;      // Mobile
			}
		}
		updatePageSize();
		window.addEventListener('resize', updatePageSize);

		return () => {
			window.removeEventListener('resize', updatePageSize);
		};
	});
</script>

<main class="container mx-auto p-4">
	<Tabs value={data.initialTab} onValueChange={handleTabChange}>
		<!-- Make tabs more compact by adjusting the width -->
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
							loading={state.loading}
							error={state.error?.message ?? null}
							onFilterChange={(key, value) => handleFilterChange(key, value)}
							onSearch={handleSearch}
						>
							<SortControls
								onSortChange={(field, direction) => {
								state.currentFilters.sort = { field, direction };
								handlePageChange(1);
							}}
							/>
						</ArtworkFilters>
					</CardContent>
				</Card>

				<!-- Results Section -->
				{#if state.loading.initialLoad}
					<div class="mt-6 text-center">
						<GridSkeleton variant="artwork" />
						<p class="text-sm text-muted-foreground mt-4">
							Loading initial artwork data...
						</p>
					</div>
				{:else if state.artworksLoading}
					<div class="mt-6">
						<GridSkeleton variant="artwork" />
					</div>
				{:else if state.error}
					<ErrorDisplay
						type={state.error.type}
						message={state.error.message}
						retryFn={state.error.retryFn}
					/>
				{:else if state.artworksData}
					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
						{#each state.artworksData.content as artwork}
							<ArtworkCard
								{artwork}
								onCardClick={handleArtworkClick}
							/>
						{/each}
					</div>
					{#if state.artworksData.totalPages > 1}
						<div class="w-full border-t mt-6">
							<div class="container mx-auto flex justify-center py-6">
								<Pagination
									count={state.artworksData?.totalElements ?? 0}
									perPage={state.pageSize}
									page={state.currentPage}
									onPageChange={(newPage) => {handlePageChange(newPage);}}
								>
									<PaginationContent class="flex items-center gap-1">
										<PaginationItem>
											<PaginationPrevButton />
										</PaginationItem>

										{#if state.artworksData.totalPages <= 7}
											{#each Array(state.artworksData.totalPages) as _, i}
												<PaginationItem>
													<PaginationLink
														page={{type: "page", value: i + 1}}
														onclick={() => handlePageChange(i + 1)}
														isActive={state.currentPage === i + 1}
													/>
												</PaginationItem>
											{/each}
										{:else}
											<!-- First page -->
											<PaginationItem>
												<PaginationLink
													page={{type: "page", value: 1}}
													isActive={state.currentPage === 1}
												/>
											</PaginationItem>

											<!-- Show ellipsis if needed -->
											{#if state.currentPage > 3}
												<PaginationItem>
													<span class="px-2">...</span>
												</PaginationItem>
											{/if}

											<!-- Pages around current -->
											{#each Array(3) as _, i}
												{#if state.currentPage - 1 + i > 1 && state.currentPage - 1 + i < state.artworksData.totalPages}
													<PaginationItem>
														<PaginationLink
															page={{type: "page", value: state.currentPage - 1 + i}}
															isActive={state.currentPage === state.currentPage - 1 + i}
														/>
													</PaginationItem>
												{/if}
											{/each}

											<!-- Show ellipsis if needed -->
											{#if state.currentPage < state.artworksData.totalPages - 2}
												<PaginationItem>
													<span class="px-2">...</span>
												</PaginationItem>
											{/if}

											<!-- Last page -->
											<PaginationItem>
												<PaginationLink
													page={{type: "page", value: state.artworksData.totalPages}}
													isActive={state.currentPage === state.artworksData.totalPages}
												/>
											</PaginationItem>
										{/if}

										<PaginationItem>
											<PaginationNextButton />
										</PaginationItem>
									</PaginationContent>
								</Pagination>
							</div>
						</div>
					{/if}
				{/if}
			</div>
		</TabsContent>

		<TabsContent value="tours">
			<div class="max-w-4xl mx-auto">
				<Card>
					<CardHeader>
						<CardTitle>Your Tours</CardTitle>
					</CardHeader>
					<CardContent class="space-y-6">
						{#if hasExistingTours}
							<TourList initialData={data?.tours ?? { content: [], totalElements: 0, totalPages: 1, size: 10, number: 0 }} />
						{:else}
							<p class="text-muted-foreground text-center">
								You haven't generated any tours yet. Create your first personalized tour experience!
							</p>
						{/if}
						<TourGenerator />
					</CardContent>
				</Card>
			</div>
		</TabsContent>
	</Tabs>
</main>

{#if state.selectedArtwork}
	<ArtworkModal
		artwork={state.selectedArtwork}
		isOpen={state.isModalOpen}
		onClose={handleCloseModal}
	/>
{/if}