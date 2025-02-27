<script lang="ts">
	import { clickOutside } from '$lib/utils/clickOutside';
	import { tick } from 'svelte';
	import { debounce } from '$lib/utils/debounce';
	import ChevronDown from 'lucide-svelte/icons/chevron-down';

	export let value: string | undefined = undefined;
	export let placeholder: string = "Select option";
	export let items: Array<{value: string, label: string, count?: number}> = [];
	export let onChange: (value: string | undefined) => void;
	export let label: string = "";
	export let loading: boolean = false;

	const ITEMS_PER_PAGE = 50;

	let dropdownRef: HTMLDivElement;
	let searchInput: HTMLInputElement;
	let searchTerm = "";
	let isOpen = false;
	let currentPage = 0;

	// Filtered items based on search
	$: filteredItems = searchTerm
		? items.filter(item =>
			item.label.toLowerCase().includes(searchTerm.toLowerCase()))
		: items;

	// Paginated items
	$: paginatedItems = filteredItems.slice(0, (currentPage + 1) * ITEMS_PER_PAGE);
	$: hasMoreItems = paginatedItems.length < filteredItems.length;

	function toggleDropdown() {
		isOpen = !isOpen;
		if (isOpen) {
			searchTerm = "";
			currentPage = 0;
			// Focus search input on open
			tick().then(() => {
				searchInput?.focus();
			});
		}
	}

	function handleClickOutside() {
		isOpen = false;
	}

	function handleSelectItem(item: {value: string, label: string}) {
		onChange(item.value);
		isOpen = false;
	}

	function loadMoreItems() {
		currentPage += 1;
	}

	const debouncedSearch = debounce((term: string) => {
		searchTerm = term;
		currentPage = 0;
	}, 150);
</script>

<div class="relative w-full" use:clickOutside={handleClickOutside}>
	<!-- Trigger button -->
	<button
		type="button"
		class="flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background
      data-[placeholder]:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
		onclick={toggleDropdown}
		aria-haspopup="listbox"
		aria-expanded={isOpen}
	>
    <span class={!value ? "text-muted-foreground" : ""}>
      {value ? items.find(i => i.value === value)?.label || value : placeholder}
    </span>
		<ChevronDown class="h-4 w-4 opacity-50" />
	</button>

	<!-- Dropdown -->
	{#if isOpen}
		<div
			bind:this={dropdownRef}
			class="absolute z-50 w-full min-w-[8rem] overflow-hidden rounded-md border border-slate-200 bg-white text-slate-950 shadow-md animate-in fade-in-80 data-[side=bottom]:slide-in-from-top-1 data-[side=left]:slide-in-from-right-1 data-[side=right]:slide-in-from-left-1 data-[side=top]:slide-in-from-bottom-1 mt-1"
		>
			<!-- Search input -->
			<div class="p-2 border-b sticky top-0 bg-background z-10">
				<input
					bind:this={searchInput}
					type="text"
					class="h-8 w-full rounded-md border border-input bg-background px-3 py-1 text-sm"
					placeholder={`Search ${label.toLowerCase()}...`}
					value={searchTerm}
					oninput={(e) => debouncedSearch(e.currentTarget.value)}
				/>
			</div>

			<!-- Loading state -->
			{#if loading}
				<div class="py-2 text-center text-sm text-muted-foreground">
					Loading options...
				</div>
				<!-- Empty state -->
			{:else if filteredItems.length === 0}
				<div class="py-2 text-center text-sm text-muted-foreground">
					No options found
				</div>
				<!-- Paginated list -->
			{:else}
				<div class="max-h-[280px] overflow-y-auto">
					{#each paginatedItems as item}
						<div
							role="option"
							aria-selected={value === item.value}
							tabindex="0"
							class="cursor-pointer hover:bg-slate-100 {value === item.value ? 'bg-slate-100' : ''}"
							onclick={() => handleSelectItem(item)}
							onkeydown={(e) => e.key === 'Enter' && handleSelectItem(item)}
						>
							<div class="flex items-center justify-between px-3 py-2">
								<span>{item.label}</span>
								{#if item.count !== undefined && item.count > 0}
									<span class="text-slate-500 text-sm">({item.count})</span>
								{/if}
							</div>
						</div>
					{/each}

					{#if hasMoreItems}
						<button
							class="w-full py-2 text-center text-sm text-primary hover:bg-slate-50 focus:bg-slate-50 focus:outline-none"
							onclick={loadMoreItems}
						>
							Load more ({filteredItems.length - paginatedItems.length} remaining)
						</button>
					{/if}
				</div>
			{/if}
		</div>
	{/if}
</div>