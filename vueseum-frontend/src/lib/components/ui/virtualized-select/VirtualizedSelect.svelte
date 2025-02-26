<script lang="ts">
	import { Select, SelectTrigger, SelectContent } from "$lib/components/ui/select";
	import { onMount } from "svelte";
	import { debounce } from "$lib/utils/debounce";

	export let value: string | undefined = undefined;
	export let placeholder: string = "Select option";
	export let items: Array<{value: string, label: string, count?: number}> = [];
	export let onChange: (value: string | undefined) => void;
	export let label: string = "";
	export let loading: boolean = false;

	const ITEM_HEIGHT = 35; // Height of each item in pixels
	const MAX_VISIBLE_ITEMS = 8; // Maximum number of visible items
	const BUFFER_SIZE = 2; // Buffer rows above and below visible area

	let containerRef: HTMLDivElement;
	let searchTerm = "";
	let scrollTop = 0;
	let isOpen = false;

	// Filtered items based on search
	$: filteredItems = searchTerm
		? items.filter(item =>
			item.label.toLowerCase().includes(searchTerm.toLowerCase()))
		: items;

	// Virtual list calculations
	$: containerHeight = Math.min(filteredItems.length, MAX_VISIBLE_ITEMS) * ITEM_HEIGHT;
	$: totalHeight = filteredItems.length * ITEM_HEIGHT;
	$: startIndex = Math.max(0, Math.floor(scrollTop / ITEM_HEIGHT) - BUFFER_SIZE);
	$: endIndex = Math.min(
		filteredItems.length - 1,
		Math.floor((scrollTop + containerHeight) / ITEM_HEIGHT) + BUFFER_SIZE
	);
	$: visibleItems = filteredItems.slice(startIndex, endIndex + 1);

	// Clear search when dropdown closes
	$: if (!isOpen) {
		searchTerm = "";
	}

	function handleScroll() {
		scrollTop = containerRef?.scrollTop ?? 0;
		console.log("Scroll:", scrollTop, "Items:", startIndex, "-", endIndex);
	}

	const debouncedSearch = debounce((term: string) => {
		searchTerm = term;
		// Reset scroll position when search changes
		if (containerRef) {
			containerRef.scrollTop = 0;
			scrollTop = 0;
		}
	}, 150);

	function handleSearchInput(e: Event) {
		const input = e.target as HTMLInputElement;
		debouncedSearch(input.value);
	}

	function handleOpenChange(open: boolean) {
		isOpen = open;
		if (open) {
			// Reset scroll position when opening
			if (containerRef) {
				containerRef.scrollTop = 0;
				scrollTop = 0;
			}

			setTimeout(() => {
				const searchInputElement = document.querySelector('.virtualized-select-search input');
				if (searchInputElement instanceof HTMLInputElement) {
					searchInputElement.focus();
				}
			}, 50);
		}
	}

	function handleSelect(value: string) {
		onChange(value);
		isOpen = false;
	}

	onMount(() => {
		console.log("VirtualizedSelect mounted", {
			itemCount: items.length,
			filteredCount: filteredItems.length,
			containerHeight,
			totalHeight
		});

		if (containerRef) {
			containerRef.addEventListener('scroll', handleScroll);

			// Initial check to ensure everything is set up correctly
			setTimeout(() => {
				if (containerRef) {
					console.log("Container scroll height:", containerRef.scrollHeight);
					console.log("Container client height:", containerRef.clientHeight);
				}
			}, 100);

			return () => {
				containerRef.removeEventListener('scroll', handleScroll);
			};
		}
	});
</script>

<div class="relative w-full">
	<Select
		type="single"
		{value}
		onOpenChange={handleOpenChange}
		onValueChange={handleSelect}
	>
		<SelectTrigger class="w-full">
      <span class={!value ? "text-muted-foreground" : ""}>
        {value ? items.find(i => i.value === value)?.label || value : placeholder}
      </span>
		</SelectTrigger>
		<SelectContent
			align="start"
			side="bottom"
			class="w-[300px] p-0"
		>
			<!-- Search input -->
			<div class="p-2 border-b sticky top-0 bg-background z-10 virtualized-select-search">
				<input
					type="text"
					class="h-8 w-full rounded-md border border-input bg-background px-3 py-1 text-sm"
					placeholder={`Search ${label.toLowerCase()}...`}
					value={searchTerm}
					oninput={handleSearchInput}
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
					style="height: {containerHeight || MAX_VISIBLE_ITEMS * ITEM_HEIGHT}px; max-height: 280px;"
				>
					<div style="height: {totalHeight}px; position: relative;">
						{#each visibleItems as item, i}
							{@const itemIndex = startIndex + i}
							<button
								type="button"
								role="option"
								aria-selected={value === item.value}
								tabindex="0"
								class="absolute w-full text-left cursor-pointer hover:bg-accent hover:text-accent-foreground
                       {value === item.value ? 'bg-accent text-accent-foreground' : ''}"
								style="top: {(itemIndex) * ITEM_HEIGHT}px; height: {ITEM_HEIGHT}px;"
								onclick={() => handleSelect(item.value)}
								onkeydown={(e) => e.key === 'Enter' && handleSelect(item.value)}
							>
								<div class="flex items-center justify-between px-3 py-2">
									<span>{item.label}</span>
									{#if item.count !== undefined}
										<span class="text-muted-foreground text-sm">({item.count})</span>
									{/if}
								</div>
							</button>
						{/each}
					</div>
				</div>
			{/if}
		</SelectContent>
	</Select>
</div>