<script lang="ts">
	import { Tabs, TabsContent, TabsList, TabsTrigger } from '$lib/components/ui/tabs';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import ArtworkCard from '$lib/components/homepage/artwork/ArtworkCard.svelte';
	import ArtworkFilters, { type SearchField } from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import ArtworkModal from '$lib/components/homepage/artwork/ArtworkModal.svelte';
	import TourList from '$lib/components/tour/TourList.svelte';
	import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
	import { goto } from '$app/navigation';
	import {
		Pagination,
		PaginationContent,
		PaginationItem,
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
	import type { PageData } from './+page';
	import { StorageManager } from '$lib/utils/storage'

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	function debounce<T extends (...args: any[]) => any>(
		fn: T,
		delay: number
	): typeof fn {  // Return the same type as the input function
		let timeoutId: ReturnType<typeof setTimeout>;
		return function(this: void, ...args: Parameters<T>) {
			clearTimeout(timeoutId);
			timeoutId = setTimeout(() => fn.apply(this, args), delay);
		} as T;  // Assert the return type matches the input function
	}

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isInitialized: false,
		selectedArtwork: null as Artwork | null,
		isModalOpen: false,
		artworksLoading: true,
		artworksData: null as PaginatedResponse<Artwork> | null,
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
		filterOptions: {
			objectType: [] as string[],
			materials: [] as string[],
			countries: [] as string[],
			regions: [] as string[],
			cultures: [] as string[]
		},
		loading: {
			options: false,
			results: false,
			initialLoad: true
		}
	});

	// Add a debounced search function
	const debouncedSearch = debounce(async (filters: typeof state.currentFilters.filters) => {
		try {
			state.artworksLoading = true;
			state.error = null;
			state.currentPage = 1;

			const results = await artworkApi.searchArtworks(
				filters,
				0,
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

	function handleSearch(filters: typeof state.currentFilters.filters) {
		debouncedSearch(filters);
	}

	// Add new function to handle filter option loading
	async function loadFilterOptions(criteria: {
		objectType?: string[];
		country?: string[];
		region?: string[];
	} = {}) {
		try {
			state.loading.options = true;
			const options = await artworkApi.getFilterOptions({
				artworkType: criteria.objectType?.[0],
				country: criteria.country?.[0],
				region: criteria.region?.[0]
			});

			// Update filter options based on the response
			state.filterOptions = {
				...state.filterOptions,
				...options
			};
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
			state.error = {
				type: 'load',
				message: 'Failed to load filter options',
				retryFn: () => loadFilterOptions(criteria)
			};
		} finally {
			state.loading.options = false;
		}
	}

	type Filters = typeof state.currentFilters.filters;

	// Update the handler for filter changes
	function handleFilterChange<K extends keyof Filters>(
		key: K,
		value: Filters[K]
	) {
		// Batch the state updates
		queueMicrotask(() => {
			state.currentFilters.filters[key] = value;

			// Load dependent options only if necessary
			if (key === 'objectType' && !state.loading.options) {
				loadFilterOptions({ objectType: value as string[] });
			} else if (key === 'country' && !state.loading.options) {
				loadFilterOptions({ country: value as string[] });
			} else if (key === 'region' && !state.loading.options) {
				loadFilterOptions({
					country: state.currentFilters.filters.country,
					region: value as string[]
				});
			}

			// Only trigger search after options are loaded
			if (!state.loading.options) {
				handleSearch(state.currentFilters.filters);
			}
		});
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
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
		} catch (error) {
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

	// Initial filter options loading
	// Ensures filter dropdowns are populated with available options
	$effect(() => {
		if (!state.filterOptions.objectType.length && !state.loading.options) {
			loadFilterOptions();
		}
	});

	// Initial state setup and URL-based loading
	$effect(() => {
		if (typeof window === 'undefined') return;
		if (data.initialTab !== 'artworks') return;
		if (state.isInitialized) return;

		// Initialize state from URL
		const url = new URL(window.location.href);

		// Set initial page
		const pageParam = url.searchParams.get('page');
		if (pageParam) {
			state.currentPage = Number(pageParam);
		}

		// Create an async initialization function
		const initializeFilters = async () => {
			state.loading.initialLoad = true;
			// Track which options we've already loaded to prevent duplicate calls
			const loadedOptions = new Set<string>();

			// Helper function to load filter options if not already loaded
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
				const filters = state.currentFilters.filters;

				// Load dependent filters in order
				if (filters.objectType.length > 0) {
					await loadOptionsIfNeeded({ objectType: filters.objectType });
				}

				if (filters.country.length > 0) {
					await loadOptionsIfNeeded({ country: filters.country });

					if (filters.region.length > 0) {
						await loadOptionsIfNeeded({
							country: filters.country,
							region: filters.region
						});
					}
				}

				state.loading.results = true;

				if (data.initialTab === 'artworks') {
					state.artworksData = await artworkApi.searchArtworks(
						filters,
						state.currentPage - 1,
						state.pageSize,
						state.currentFilters.sort.field !== 'relevance' ? {
							field: state.currentFilters.sort.field,
							direction: state.currentFilters.sort.direction
						} : undefined
					);
				}

				state.isInitialized = true;
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

		// Start initialization
		initializeFilters();
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

	// Load saved filters on mount
	$effect(() => {
		if (typeof window === 'undefined') return;

		const savedFilters = StorageManager.get<ArtworkFilters>('lastFilters', state.currentFilters.filters);
		if (savedFilters && !state.isInitialized) {
			state.currentFilters.filters = savedFilters;
		}
	});

	let initialLoad = true;
	const debouncedUrlUpdate = debounce(updateUrlParams, 300);

	$effect(() => {
		if (typeof window === 'undefined') return;
		if (data.initialTab !== 'artworks') return;

		// Handle initial load
		if (initialLoad) {
			initialLoad = false;

			// Load saved filters first with default values
			const savedFilters = StorageManager.get<ArtworkFilters>('lastFilters', {
				...data.initialFilters  // Use the initial filters as default
			});

			// Then merge with URL parameters if they exist
			const url = new URL(window.location.href);
			const urlSearchTerm = url.searchParams.get('q');
			const urlSearchField = url.searchParams.get('searchField');

			// Merge URL parameters with saved filters, preferring URL parameters
			state.currentFilters.filters = {
				...savedFilters,
				...(urlSearchTerm || urlSearchField ? {
					searchTerm: urlSearchTerm ? [urlSearchTerm] : [''],
					searchField: (urlSearchField ?? 'all') as SearchField,
				} : {}),
			};

			// Initialize state and trigger search
			state.isInitialized = true;
			if (!state.artworksData) {
				handleSearch(state.currentFilters.filters);
			}
			return;
		}

		// Handle subsequent updates
		if (!state.isInitialized) return;

		// Update storage
		StorageManager.set('lastFilters', state.currentFilters.filters);

		// Update URL (debounced)
		const params = {
			q: state.currentFilters.filters.searchTerm[0] || null,
			searchField: state.currentFilters.filters.searchField === 'all' ?
				null : state.currentFilters.filters.searchField,
			objectType: state.currentFilters.filters.objectType,
			medium: state.currentFilters.filters.materials,
			country: state.currentFilters.filters.country,
			region: state.currentFilters.filters.region,
			culture: state.currentFilters.filters.culture,
			period: state.currentFilters.filters.era,
			sortBy: state.currentFilters.sort.field !== 'relevance' ?
				state.currentFilters.sort.field : null,
			sortDirection: state.currentFilters.sort.field !== 'relevance' ?
				state.currentFilters.sort.direction : null,
			page: state.currentPage > 1 ? state.currentPage.toString() : null
		};

		debouncedUrlUpdate(params);
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
							onFilterChange={handleFilterChange}
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
						<div class="flex justify-center mt-6">
							<Pagination
								count={state.artworksData?.totalElements ?? 0}
								perPage={state.pageSize}
								page={state.currentPage}
								onPageChange={(newPage) => {handlePageChange(newPage);}}
							>
							<PaginationContent>
								<PaginationItem>
									<PaginationPrevButton />
								</PaginationItem>

								{#each Array(state.artworksData.totalPages) as _, i}
									<PaginationItem>
										<PaginationLink
											page={{type: "page", value: i + 1}}
											onclick={() => handlePageChange(i + 1)}
											isActive={state.currentPage === i + 1}
										>
											{i + 1}
										</PaginationLink>
									</PaginationItem>
								{/each}

								<PaginationItem>
									<PaginationNextButton />
								</PaginationItem>
							</PaginationContent>
							</Pagination>
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