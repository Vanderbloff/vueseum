<script lang="ts">
	import { clickOutside } from '$lib/utils/clickOutside';
	import { onMount, tick } from 'svelte';
	import { debounce } from '$lib/utils/debounce';
	import ChevronDown from 'lucide-svelte/icons/chevron-down';

	export let value: string | undefined = undefined;
	export let placeholder: string = "Select option";
	export let items: Array<{value: string, label: string, count?: number}> = [];
	export let onChange: (value: string | undefined) => void;
	export let label: string = "";
	export let loading: boolean = false;

	const ITEM_HEIGHT = 35;
	const MAX_VISIBLE_ITEMS = 8;
	const BUFFER_ITEMS = 4; // Extra items to render above and below viewport

	let containerRef: HTMLDivElement;
	let dropdownRef: HTMLDivElement;
	let searchInput: HTMLInputElement;
	let searchTerm = "";
	let scrollTop = 0;
	let isOpen = false;

	// Filtered items based on search
	$: filteredItems = searchTerm
		? items.filter(item =>
			item.label.toLowerCase().includes(searchTerm.toLowerCase()))
		: items;

	// Virtual list calculations
	$: containerHeight = Math.min(filteredItems.length * ITEM_HEIGHT, MAX_VISIBLE_ITEMS * ITEM_HEIGHT);
	$: totalHeight = filteredItems.length * ITEM_HEIGHT;
	$: startIndex = Math.max(0, Math.floor(scrollTop / ITEM_HEIGHT) - BUFFER_ITEMS);
	$: endIndex = Math.min(
		filteredItems.length - 1,
		Math.floor((scrollTop + containerHeight) / ITEM_HEIGHT) + BUFFER_ITEMS
	);
	$: visibleItems = filteredItems.slice(startIndex, endIndex + 1);

	function handleScroll() {
		if (containerRef) {
			scrollTop = containerRef.scrollTop;
		}
	}

	function toggleDropdown() {
		isOpen = !isOpen;
		if (isOpen) {
			searchTerm = "";
			// Focus search input on open
			tick().then(() => {
				searchInput?.focus();
				if (containerRef) {
					containerRef.scrollTop = 0;
					scrollTop = 0;
				}
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

	function handleKeyDown(e: KeyboardEvent) {
		if (!isOpen) return;

		if (e.key === 'Escape') {
			isOpen = false;
			e.preventDefault();
		}
	}

	onMount(() => {
		if (containerRef) {
			containerRef.addEventListener('scroll', handleScroll, { passive: true });
			return () => {
				containerRef.removeEventListener('scroll', handleScroll);
			};
		}
	});

	const debouncedSearch = debounce((term: string) => {
		searchTerm = term;
		if (containerRef) {
			containerRef.scrollTop = 0;
			scrollTop = 0;
		}
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
			role="listbox"
			tabindex="0"
			onkeydown={handleKeyDown}
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
				<!-- Virtualized list -->
			{:else}
				<div
					bind:this={containerRef}
					class="overflow-y-auto"
					style="height: {containerHeight}px; max-height: 280px;"
				>
					<div style="height: {totalHeight}px; position: relative; will-change: transform;">
						{#each visibleItems as item, i}
							{@const itemIndex = startIndex + i}
							<div
								role="option"
								aria-selected={value === item.value}
								tabindex="0"
								class="absolute w-full text-left cursor-pointer hover:bg-slate-100
                 {value === item.value ? 'bg-slate-100' : ''}"
								style="top: {(itemIndex) * ITEM_HEIGHT}px; height: {ITEM_HEIGHT}px; left: 0; right: 0;"
								onclick={() => handleSelectItem(item)}
								onkeydown={(e) => e.key === 'Enter' && handleSelectItem(item)}
							>
								<div class="flex items-center justify-between px-3 py-2">
									<span>{item.label}</span>
									{#if item.count !== undefined}
										<span class="text-slate-500 text-sm">({item.count})</span>
									{/if}
								</div>
							</div>
						{/each}
					</div>
				</div>
			{/if}
		</div>
	{/if}
</div>