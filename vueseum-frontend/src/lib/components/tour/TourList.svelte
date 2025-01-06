<!--// src/lib/components/tour/TourList.svelte-->
<script lang="ts">
	import { Skeleton } from "$lib/components/ui/skeleton";
	import TourCard from './TourCard.svelte';
	import type { Tour, PaginatedResponse } from '$lib/types/tour';
	import { getMockPaginatedTours } from '$lib/mocks/TourData';

	export let initialData: PaginatedResponse<Tour>;

	// Component state
	let tours: Tour[] = initialData.content;
	let isLoading = false;
	let error: string | null = null;
	let currentPage = initialData.number;
	let totalPages = initialData.totalPages;

	async function loadTours(page = 0) {
		isLoading = true;
		error = null;

		try {
				await new Promise(resolve => setTimeout(resolve, 1000));
				const data = getMockPaginatedTours(page);
				tours = data.content;
				totalPages = data.totalPages;
				currentPage = data.number;
		} catch (e) {
				error = e instanceof Error ? e.message : 'An error occurred';
				tours = [];
		} finally {
				isLoading = false;
		}
	}

	// Handle page changes
	async function changePage(newPage: number) {
		if (newPage >= 0 && newPage < totalPages) {
			await loadTours(newPage);
		}
	}
</script>

<div class="w-full space-y-6">
	<!-- Error state -->
	{#if error}
		<div class="p-4 bg-red-50 border border-red-100 rounded-lg">
			<p class="text-red-700">{error}</p>
			<button
				class="mt-2 text-sm text-red-700 underline hover:text-red-800"
				on:click={() => loadTours(currentPage)}
			>
				Try again
			</button>
		</div>
	{/if}

	{#if isLoading}
		<div class="grid gap-4 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each [...Array(6).keys()] as _}
				<div class="space-y-4">
					<Skeleton class="h-24 w-full" />
					<div class="space-y-2">
						<Skeleton class="h-4 w-3/4" />
						<Skeleton class="h-3 w-1/2" />
					</div>
				</div>
			{/each}
		</div>
	{:else if tours.length === 0 && !error}
		<div class="text-center py-12">
			<p class="text-gray-500">No tours available yet.</p>
			<p class="text-gray-500 mt-2">Check back later for new tours.</p>
		</div>
	{:else}
		<div class="grid gap-4 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each tours as tour (tour.id)}
				<TourCard {tour} />
			{/each}
		</div>
	{/if}

	{#if totalPages > 1}
		<div class="flex justify-center gap-2 mt-6">
			<button
				class="px-3 py-1 rounded bg-gray-100 disabled:opacity-50
                       transition-colors duration-200 hover:bg-gray-200"
				disabled={currentPage === 0 || isLoading}
				on:click={() => changePage(currentPage - 1)}
			>
				Previous
			</button>

			<span class="px-3 py-1">
                Page {currentPage + 1} of {totalPages}
            </span>

			<button
				class="px-3 py-1 rounded bg-gray-100 disabled:opacity-50
                       transition-colors duration-200 hover:bg-gray-200"
				disabled={currentPage === totalPages - 1 || isLoading}
				on:click={() => changePage(currentPage + 1)}
			>
				Next
			</button>
		</div>
	{/if}
</div>