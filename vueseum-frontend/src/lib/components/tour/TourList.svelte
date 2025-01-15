<script lang="ts">
	import TourCard from './TourCard.svelte';
	import type { Tour, PaginatedResponse } from '$lib/types/tour';
	import { Button } from "$lib/components/ui/button";
	import GridSkeleton from '$lib/components/shared/GridSkeleton.svelte';

	let { initialData } = $props<{
		initialData: PaginatedResponse<Tour>;
	}>();

	const state = $state({
		tours: initialData.content,
		isLoading: false,
		error: null as string | null,
		updateError: null as string | null,
		updatingTourId: null as number | null
	});

	async function loadTours() {
		state.isLoading = true;
		state.error = null;

		try {
			const response = await fetch('/api/v1/tours');
			if (!response.ok) {
				throw new Error('Failed to load tours');
			}
			const data = await response.json();
			state.tours = data.content;
		} catch (e) {
			state.error = e instanceof Error ? e.message : 'An error occurred';
			state.tours = [];
		} finally {
			state.isLoading = false;
		}
	}

	async function handleDelete(tourId: number) {
		try {
			const response = await fetch(`/api/v1/tours/${tourId}`, {
				method: 'DELETE'
			});

			if (!response.ok) {
				throw new Error('Failed to delete tour');
			}

			// Remove from local state
			state.tours = state.tours.filter((tour: Tour) => tour.id !== tourId);
		} catch (error) {
			state.error = error instanceof Error ? error.message : 'Failed to delete tour';
		}
	}

	async function handleTourEdit(tourId: number, updates: { name: string; description: string }) {
		state.updateError = null;
		state.updatingTourId = tourId;

		try {
			const response = await fetch(`/api/v1/tours/${tourId}`, {
				method: 'PATCH',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(updates)
			});

			if (!response.ok) {
				throw new Error('Failed to update tour');
			}

			const updatedTour = await response.json();

			// Update local state
			state.tours = state.tours.map((tour: Tour) =>
				tour.id === tourId ? { ...tour, ...updatedTour } : tour
			);

			return true;
		} catch (error) {
			console.error('Error updating tour:', error);
			state.updateError = error instanceof Error ? error.message : 'Failed to update tour';
			throw error;
		} finally {
			state.updatingTourId = null;
		}
	}
</script>

<div class="w-full space-y-6">
	<!-- Main error state -->
	{#if state.error}
		<div class="p-4 bg-red-50 border border-red-100 rounded-lg">
			<p class="text-red-700">{state.error}</p>
			<Button
				variant="link"
				class="mt-2 text-red-700 hover:text-red-800"
				onclick={loadTours}
			>
				Try again
			</Button>
		</div>
	{/if}

	{#if state.updateError}
		<div class="p-4 bg-red-50 border border-red-100 rounded-lg">
			<p class="text-red-700">{state.updateError}</p>
			<Button
				variant="link"
				class="mt-2 text-red-700 hover:text-red-800"
				onclick={() => state.updateError = null}
			>
				Dismiss
			</Button>
		</div>
	{/if}

	{#if state.isLoading}
		<GridSkeleton variant="tour" />
	{:else if state.tours.length === 0 && !state.error}
		<div class="text-center py-12">
			<p class="text-gray-500">No tours available yet.</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
			{#each state.tours as tour (tour.id)}
				<TourCard
					{tour}
					onDelete={handleDelete}
					onEdit={handleTourEdit}
					isUpdating={state.updatingTourId === tour.id}
				/>
			{/each}
		</div>
	{/if}
</div>