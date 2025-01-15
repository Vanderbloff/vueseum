<script lang="ts">
	import { Tabs, TabsList, TabsTrigger, TabsContent } from "$lib/components/ui/tabs";
	import { Card, CardContent, CardHeader, CardTitle } from "$lib/components/ui/card";
	import ArtworkCard from '$lib/components/homepage/artwork/ArtworkCard.svelte';
	import ArtworkFilters from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import ArtworkModal from '$lib/components/homepage/artwork/ArtworkModal.svelte';
	import TourList from '$lib/components/tour/TourList.svelte';
	import { getMockPaginatedTours } from '$lib/mocks/TourData';
	import type { Artwork, PaginatedResponse, StandardPeriod } from '$lib/types/artwork';
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
	import { ArtworkUtils } from '$lib/utils/artwork/artworkUtils';

	let { data } = $props();

	const state = $state({
		selectedArtwork: null as Artwork | null,
		isModalOpen: false,
		artworksLoading: false,
		artworksData: null as PaginatedResponse<Artwork> | null,
		currentPage: 1,
		pageSize: 3,
		error: null as {
			type: 'search' | 'load' | 'pagination',
			message: string,
			retryFn?: () => void } | null,
		currentFilters: {
			filters: {
				searchTerm: [] as string[],
				searchField: 'all' as 'all' | 'title' | 'artist' | 'culture',
				objectType: [] as string[],
				materials: [] as string[],
				geographicLocation: [] as string[],
				culture: [] as string[],
				era: [] as StandardPeriod[],
				onDisplay: false,
				hasImage: true
			},
			sort: {
				field: 'relevance' as 'relevance' | 'title' | 'artist' | 'date',
				direction: 'asc' as 'asc' | 'desc'
			}
		}
	});

	async function handlePageChange(newPage: number) {
		try {
			state.artworksLoading = true;
			state.error = null;

			// Get all results if we're sorting
			const isSorting = state.currentFilters.sort.field !== 'relevance';
			let allResults = await artworkApi.searchArtworks(
				state.currentFilters.filters,
				isSorting ? 0 : newPage - 1,  // If sorting, get all results
				isSorting ? 999 : state.pageSize
			);

			if (!allResults || !allResults.content) {
				throw new Error('Invalid response data');
			}

			// Apply sorting if needed
			if (isSorting) {
				allResults.content = ArtworkUtils.sortArtworks(
					allResults.content,
					state.currentFilters.sort.field,
					state.currentFilters.sort.direction
				);

				// Manual pagination after sorting
				const startIndex = (newPage - 1) * state.pageSize;
				const paginatedContent = allResults.content.slice(
					startIndex,
					startIndex + state.pageSize
				);

				// Handle empty results with context
				if (paginatedContent.length === 0) {
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

				allResults = {
					...allResults,
					content: paginatedContent,
					totalElements: allResults.content.length,
					totalPages: Math.ceil(allResults.content.length / state.pageSize)
				};
			}

			state.artworksData = allResults;
			state.currentPage = newPage;
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
	    (data?.tours ?? getMockPaginatedTours(0)).content.length > 0
	 );
</script>

<main class="container mx-auto p-4">
	<h1 class="text-2xl font-bold mb-4">Vueseum</h1>

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
					<!--<CardHeader>
						<CardTitle>Search & Filter Artworks</CardTitle>
					</CardHeader>-->
					<CardContent>
						<ArtworkFilters
							onSearch={(filters) => {
								state.artworksLoading = true;
								state.error = null;
								state.currentFilters.filters = filters;
								state.currentPage = 1;

								artworkApi.searchArtworks(
										state.currentFilters.filters,
										0,  // first page
										state.pageSize
								).then(results => {
										if (results.totalElements === 0) {
												state.error = {
														type: 'search',
														message: 'No artworks match your search criteria. Try adjusting your filters.'
												};
												return;
										}
										state.artworksData = results;
								}).catch(error => {
										console.error('Error performing search:', error);
										state.error = {
												type: 'search',
												message: 'An error occurred while searching. Please try again.',
												retryFn: () => {
														state.artworksLoading = true;
														state.error = null;
														state.currentPage = 1;
														artworkApi.searchArtworks(filters, 0, state.pageSize);
												}
										};
								}).finally(() => {
										state.artworksLoading = false;
								});
						}}
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
				{#if state.artworksLoading}
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
							<TourList initialData={data?.tours ?? getMockPaginatedTours(0)} />
						{:else}
							<p class="text-muted-foreground text-center">
								You haven't generated any tours yet. Create your first personalized tour experience!
							</p>
						{/if}
						<TourGenerator
								onTourGenerated={() => {
									data.tours = getMockPaginatedTours(0);
							}}
						/>
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