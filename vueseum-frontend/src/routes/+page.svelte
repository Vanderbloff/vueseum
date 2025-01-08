<script lang="ts">
	import { Tabs, TabsList, TabsTrigger, TabsContent } from "$lib/components/ui/tabs";
	import { Card, CardContent, CardHeader, CardTitle } from "$lib/components/ui/card";
	import ArtworkCard from '$lib/components/homepage/artwork/ArtworkCard.svelte';
	import ArtworkFilters from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
	import ArtworkModal from '$lib/components/homepage/artwork/ArtworkModal.svelte';
	import TourList from '$lib/components/tour/TourList.svelte';
	import { getMockPaginatedTours } from '$lib/mocks/TourData';
	import type { Artwork, PaginatedResponse } from '$lib/types/artwork';
	import { goto } from '$app/navigation';
	import { getMockPaginatedArtworks } from '$lib/mocks/ArtworkData';
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
				searchField: 'all' as 'all' | 'title' | 'artist' | 'medium',
				objectType: [] as string[],
				culturalRegion: [] as string[],
				era: [] as string[],
				department: [] as string[],
				onDisplay: false,
				hasImage: true
			},
			sort: {
				field: 'relevance' as 'relevance' | 'title' | 'artist' | 'date',
				direction: 'asc' as 'asc' | 'desc'
			}
		}
	});

	async function handlePageChange(page: number) {
		state.artworksLoading = true;
		state.error = null;
		state.currentPage = page;

		try {
			const newData = getMockPaginatedArtworks(
				page - 1,
				state.currentFilters.filters,
				state.pageSize,
				state.currentFilters.sort
			);

			if (newData.content.length === 0) {
				state.error = {
					type: 'search',
					message: 'No artworks found for the current page',
					retryFn: () => handlePageChange(1) // Reset to first page as recovery
				};
				return;
			}

			state.artworksData = newData;
		} catch (error) {
			console.error('Error fetching artworks:', error);
			state.error = {
				type: 'pagination',
				message: 'Failed to load the requested page. Please try again.',
				retryFn: () => handlePageChange(page)
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

								setTimeout(async () => {
										try {
												const results = getMockPaginatedArtworks(
														0,
														filters,
														state.pageSize,
														state.currentFilters.sort
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
														retryFn: () => {
																state.artworksLoading = true;
																state.error = null;
																state.currentPage = 1;
																getMockPaginatedArtworks(0, filters, state.pageSize, state.currentFilters.sort);
																state.artworksLoading = false;
														}
												};
										} finally {
												state.artworksLoading = false;
										}
								}, 1500);
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