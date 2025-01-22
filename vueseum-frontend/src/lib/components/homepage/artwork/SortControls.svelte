
<script module lang="ts">
	export const SORT_OPTIONS = [
		{ value: 'relevance', label: 'Relevance' },
		{ value: 'title', label: 'Title (A-Z)' },
		{ value: 'artist', label: 'Artist Name (A-Z)' },
		{ value: 'date', label: 'Date (Newest)' }
	] as const;

	export type SortField = (typeof SORT_OPTIONS)[number]['value'];
	export type SortDirection = 'asc' | 'desc';
	export type SortConfig = {
		field: SortField;
		direction: SortDirection;
	};
</script>

<script lang="ts">
	import { Select, SelectContent, SelectItem, SelectTrigger } from "$lib/components/ui/select";
	import { Button } from "$lib/components/ui/button";
	import { ArrowUpDown } from "lucide-svelte";
	import { cn } from "$lib/utils";

	let { onSortChange } = $props<{
		onSortChange: (field: SortField, direction: SortDirection) => void;
	}>();

	const state = $state({
		sort: {
			field: 'relevance' as SortField,
			direction: 'asc' as SortDirection
		}
	});

	function isSortField(value: string): value is SortField {
		return SORT_OPTIONS.some(opt => opt.value === value);
	}

	// Function to get display text based on current sort
	function getSortDisplay(field: SortField, direction: SortDirection) {
		const option = SORT_OPTIONS.find(opt => opt.value === field);
		if (!option) return '';

		switch (field) {
			case 'relevance':
				return option.label;
			case 'title':
			case 'artist':
				return option.label.replace(
					'A-Z',
					direction === 'asc' ? 'A-Z' : 'Z-A'
				);
			case 'date':
				return option.label.replace(
					'Newest',
					direction === 'asc' ? 'Oldest' : 'Newest'
				);
			default:
				return option.label;
		}
	}

	function getDefaultDirection(field: SortField): SortDirection {
		switch (field) {
			case 'date':
				return 'desc';  // Newest first
			case 'title':
			case 'artist':
				return 'asc';   // A-Z
			default:
				return 'asc';   // Default ascending
		}
	}

	function handleFieldChange(newValue: string) {
		if (isSortField(newValue)) {
			const defaultDirection = getDefaultDirection(newValue);
			state.sort.field = newValue;
			state.sort.direction = defaultDirection;
			onSortChange(newValue, defaultDirection);
		}
	}

	function toggleDirection() {
		if (state.sort.field === 'relevance') return;
		state.sort.direction = state.sort.direction === 'asc' ? 'desc' : 'asc';
		onSortChange(state.sort.field, state.sort.direction);
	}
</script>

<div class="flex items-center gap-2">
	<Select
		type="single"
		value={state.sort.field}
		onValueChange={handleFieldChange}
	>
		<SelectTrigger class="w-[180px]">
			{getSortDisplay(state.sort.field, state.sort.direction)}
		</SelectTrigger>
		<SelectContent>
			{#each SORT_OPTIONS as option}
				<SelectItem value={option.value}>
					{option.label}
				</SelectItem>
			{/each}
		</SelectContent>
	</Select>

	<Button
		variant="ghost"
		size="icon"
		class={cn(
			"transition-opacity",
			state.sort.field === 'relevance' ? "opacity-50 cursor-not-allowed" : "hover:opacity-70"
		)}
		disabled={state.sort.field === 'relevance'}
		onclick={toggleDirection}
	>
		<ArrowUpDown class={cn(
			"h-4 w-4 transition-transform",
			state.sort.direction === 'desc' && "rotate-180"
	)} />
	</Button>
</div>