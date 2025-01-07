<!--// src/lib/components/tour/TourList.svelte-->
<script lang="ts">
	import { Skeleton } from "$lib/components/ui/skeleton";
	import TourCard from './TourCard.svelte';
	import type { Tour, PaginatedResponse } from '$lib/types/tour';
	import { getMockPaginatedTours } from '$lib/mocks/TourData';
	import { Button } from "$lib/components/ui/button";

	let { initialData } = $props<{
		initialData: PaginatedResponse<Tour>;
	}>();

	const state = $state({
		tours: initialData.content,
		isLoading: false,
		error: null as string | null,
		currentPage: initialData.number,
		totalPages: initialData.totalPages
	});

	async function loadTours(page = 0) {
		state.isLoading = true;
		state.error = null;

		try {
			await new Promise(resolve => setTimeout(resolve, 1000));
			const data = getMockPaginatedTours(page);
			state.tours = data.content;
			state.totalPages = data.totalPages;
			state.currentPage = data.number;
		} catch (e) {
			state.error = e instanceof Error ? e.message : 'An error occurred';
			state.tours = [];
		} finally {
			state.isLoading = false;
		}
	}

	async function changePage(newPage: number) {
		if (newPage >= 0 && newPage < state.totalPages) {
			await loadTours(newPage);
		}
	}

	function handleDelete(tourId: number) {
		// Remove from local state
		state.tours = state.tours.filter((tour : Tour) => tour.id !== tourId);
	}

</script>

<div class="w-full space-y-6">
	<!-- Error state -->
	{#if state.error}
		<div class="p-4 bg-red-50 border border-red-100 rounded-lg">
			<p class="text-red-700">{state.error}</p>
			<Button
				variant="link"
				class="mt-2 text-red-700 hover:text-red-800"
				onclick={() => loadTours(state.currentPage)}
			>
				Try again
			</Button>
		</div>
	{/if}

	{#if state.isLoading}
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
	{:else if state.tours.length === 0 && !state.error}
		<div class="text-center py-12">
			<p class="text-gray-500">No tours available yet.</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
			{#each state.tours as tour (tour.id)}
				<TourCard
					{tour}
					onDelete={(id) => handleDelete(Number(id))}
				/>
			{/each}
		</div>
	{/if}

	{#if state.totalPages > 1}
		<div class="flex justify-center gap-2 mt-6">
			<Button
				variant="outline"
				disabled={state.currentPage === 0 || state.isLoading}
				onclick={() => changePage(state.currentPage - 1)}
			>
				Previous
			</Button>

			<span class="px-3 py-1">
                Page {state.currentPage + 1} of {state.totalPages}
            </span>

			<Button
				variant="outline"
				disabled={state.currentPage === state.totalPages - 1 || state.isLoading}
				onclick={() => changePage(state.currentPage + 1)}
			>
				Next
			</Button>
		</div>
	{/if}
</div>