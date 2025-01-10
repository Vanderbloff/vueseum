<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Input } from '$lib/components/ui/input';
	import { mockFetchSuggestions } from '$lib/mocks/MockSuggestions';

	type SuggestionType = 'ARTWORK' | 'ARTIST' | 'MEDIUM' | 'CULTURE' | 'PERIOD';

	interface Suggestion {
		value: string;
		display: string;
		count: number;
		type: SuggestionType;
	}

	let {
		label,
		type,
		placeholder,
		museumId
	} = $props<{
		label: string;
		type: SuggestionType;
		placeholder: string;
		museumId: string;
	}>();

	const state = $state({
		inputValue: '',
		suggestions: [] as Suggestion[],
		isLoading: false,
		showSuggestions: false,
		selectedValues: [] as string[],
		isFocused: false
	});

	async function fetchSuggestions(prefix: string) {
		if (!museumId || prefix.length < 2) {
			state.suggestions = [];
			return;
		}

		if (import.meta.env.DEV) {
			const suggestions = await mockFetchSuggestions(prefix, type);
			state.suggestions = suggestions;
			state.showSuggestions = suggestions.length > 0;
			return;
		}

		state.isLoading = true;
		try {
			const params = new URLSearchParams({
				prefix,
				type,
				museumId,
				...(state.selectedValues.length > 0 && {
					[`preferred${type.charAt(0).toUpperCase() + type.slice(1).toLowerCase()}s`]: state.selectedValues.join(',')
				})
			});

			const response = await fetch(`/api/v1/suggestions?${params}`);
			if (!response.ok) throw new Error('Failed to fetch suggestions');

			state.suggestions = await response.json() as Suggestion[];
			state.showSuggestions = state.suggestions.length > 0;
		} catch (error) {
			console.error('Error fetching suggestions:', error);
			state.suggestions = [];
		} finally {
			state.isLoading = false;
		}
	}

	function handleInputChange(value: string) {
		state.inputValue = value;
		if (value) {
			fetchSuggestions(value);
		} else {
			state.suggestions = [];
			state.showSuggestions = false;
		}
	}

	function handleBlur() {
		// Small delay to allow for suggestion click to register
		setTimeout(() => {
			state.showSuggestions = false;
			state.isFocused = false;
		}, 150);
	}

	function handleFocus() {
		state.isFocused = true;
		// Only show suggestions if we have a valid input value
		if (state.inputValue.length >= 2) {
			fetchSuggestions(state.inputValue);
		}
	}

	function handleSelectSuggestion(value: string) {
		const suggestion = state.suggestions.find(s => s.value === value);
		if (suggestion && !state.selectedValues.includes(suggestion.value)) {
			state.selectedValues = [...state.selectedValues, suggestion.value];
		}
		state.inputValue = '';
		state.showSuggestions = false;
		state.suggestions = [];
	}

	function handleRemoveValue(valueToRemove: string) {
		state.selectedValues = state.selectedValues.filter(value => value !== valueToRemove);
	}

	// Expose method to get current selections
	export function getSelections(): string[] {
		return [...state.selectedValues];
	}

	// Method to clear selections (e.g., after form submission)
	export function clearSelections(): void {
		state.selectedValues = [];
		state.inputValue = '';
		state.suggestions = [];
		state.showSuggestions = false;
	}

	// Effect for input value changes
	$effect(() => {
		handleInputChange(state.inputValue);
	});
</script>

<div class="space-y-2">
	<Label>{label}</Label>
	<div class="relative">
		<Input
			placeholder={placeholder}
			value={state.inputValue}
			oninput={(e) => handleInputChange(e.currentTarget.value)}
			onblur={handleBlur}
			onfocus={handleFocus}
		/>

		{#if state.isLoading}
			<div class="absolute right-2 top-2">
				<span class="loading loading-spinner loading-sm"></span>
			</div>
		{/if}

		{#if state.selectedValues.length > 0}
			<div class="flex flex-wrap gap-1 mt-2">
				{#each state.selectedValues as value}
					<Badge variant="secondary">
						{value}
						<Button
							variant="ghost"
							size="icon"
							class="h-4 w-4 p-0 hover:bg-transparent hover:text-destructive"
							onclick={() => handleRemoveValue(value)}
						>
							×
						</Button>
					</Badge>
				{/each}
			</div>
		{/if}

		{#if state.showSuggestions}
			<div class="absolute w-full z-50 bg-background border rounded-md shadow-md mt-1">
				<div class="p-1">
					{#each state.suggestions as suggestion}
						<button
							class="w-full text-left px-2 py-1 hover:bg-accent rounded-sm"
							onclick={() => handleSelectSuggestion(suggestion.value)}
						>
							{suggestion.display}
						</button>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</div>