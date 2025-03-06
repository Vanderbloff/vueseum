<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Label } from '$lib/components/ui/label';
	import { Badge } from '$lib/components/ui/badge';
	import { Input } from '$lib/components/ui/input';
	import type { TourPreferences } from '$lib/types/tour-preferences';
	import { suggestionApi } from '$lib/api/suggestion';
	import { ApiError } from '$lib/api/base';

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
		museumId,
		currentPreferences
	} = $props<{
		label: string;
		type: SuggestionType;
		placeholder: string;
		museumId: string;
		currentPreferences: Partial<TourPreferences>;
	}>();

	const state = $state({
		inputValue: '',
		suggestions: [] as Suggestion[],
		isLoading: false,
		showSuggestions: false,
		selectedValues: [] as string[],
		isFocused: false,
		error: null as string | null
	});

	async function fetchSuggestions(prefix: string) {
		if (!museumId || prefix.length < 2) {
			state.suggestions = [];
			return;
		}

		state.isLoading = true;
		state.error = null;

		try {
			const suggestions = await suggestionApi.getSuggestions(
				prefix,
				type,
				museumId,
				currentPreferences
			);

			state.suggestions = suggestions;
			state.showSuggestions = suggestions.length > 0;
		} catch (error) {
			if (error instanceof ApiError) {
				state.error = 'Failed to load suggestions. Please try again.';
			} else {
				state.error = 'An unexpected error occurred';
			}
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
{#if state.error}
	<div class="text-sm text-destructive mt-1">
		{state.error}
	</div>
{/if}
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
		<small class="text-xs text-muted-foreground mt-1 block">
			Only showing options with available artwork images
		</small>

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