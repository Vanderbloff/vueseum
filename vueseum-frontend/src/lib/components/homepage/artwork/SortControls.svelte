<script lang="ts">
	import { Select, SelectContent, SelectItem, SelectTrigger } from "$lib/components/ui/select";
	import { Button } from "$lib/components/ui/button";
	import { ArrowUpDown } from "lucide-svelte";
	import { cn } from "$lib/utils";

	const SORT_OPTIONS = [
		{ value: 'relevance', label: 'Relevance' },
		{ value: 'title', label: 'Title (A-Z)' },
		{ value: 'artist', label: 'Artist Name (A-Z)' },
		{ value: 'date', label: 'Date (Newest)' }
	] as const;

	type SortField = (typeof SORT_OPTIONS)[number]['value'];

	let { onSortChange } = $props<{
		onSortChange: (field: SortField, direction: 'asc' | 'desc') => void;
	}>();

	const state = $state({
		sort: {
			field: 'relevance' as SortField,
			direction: 'asc' as 'asc' | 'desc'
		}
	});

	// Function to get display text based on current sort
	function getSortDisplay(field: SortField, direction: 'asc' | 'desc') {
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

	// Toggle direction and notify parent
	function toggleDirection() {
		if (state.sort.field === 'relevance') return;
		state.sort.direction = state.sort.direction === 'asc' ? 'desc' : 'asc';
		onSortChange(state.sort.field, state.sort.direction);
	}

	// Handle field change and notify parent
	function handleFieldChange(newValue: string) {
		// Validate that the new value is a valid sort field
		if (SORT_OPTIONS.some(opt => opt.value === newValue)) {
			const newField = newValue as SortField;
			const defaultDirection = getDefaultDirection(newField);
			state.sort.field = newField;
			state.sort.direction = defaultDirection;
			onSortChange(newField, defaultDirection);
		}
	}

	function getDefaultDirection(field: SortField): 'asc' | 'desc' {
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