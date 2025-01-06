<!-- src/lib/components/homepage/TabbedInterface.svelte -->
<script lang="ts">
	// Import our icons from lucide-svelte
	import { writable } from 'svelte/store';
	import { Camera, Compass } from 'lucide-svelte';

	// Our Tab interface using union types for the icon
	interface Tab {
		id: string;
		label: string;
		icon?: typeof Camera | typeof Compass;  // Can be either icon component or undefined
	}

	// Props
	let {
		initialActiveTab = 'artworks',
		onTabChange,
		artworks,
		tours
	} = $props();

	// Now we use the actual component references, not strings
	const tabs: Tab[] = [
		{ id: 'artworks', label: 'Search Artworks', icon: Camera },
		{ id: 'tours', label: 'Tours', icon: Compass }
	];

	// Track the active tab
	const activeTab = writable(initialActiveTab);

</script>

<div class="tab-container bg-gray-900/80 backdrop-blur-lg rounded-lg p-4 shadow-xl">
	<nav class="flex gap-2 mb-4">
		{#each tabs as tab}
			<button
				class="px-4 py-2 rounded-full transition-all duration-200
                       {$activeTab === tab.id ?
                         'bg-white/10 text-white' :
                         'text-gray-400 hover:text-white hover:bg-white/5'}"
				onclick={() => {
                    activeTab.set(tab.id);
                    onTabChange?.(tab.id);
                }}
				aria-selected={$activeTab === tab.id}
				role="tab"
			>
				{#if tab.icon}
					{#if tab.icon === Camera}
						<Camera size={16} />
					{:else}
						<Compass size={16} />
					{/if}
				{/if}
				{tab.label}
			</button>
		{/each}
	</nav>

	<div class="tab-content">
		<div
			id="panel-artworks"
			role="tabpanel"
			aria-labelledby="tab-artworks"
			class="transition-opacity duration-200"
			class:opacity-0={$activeTab !== 'artworks'}
			class:hidden={$activeTab !== 'artworks'}
		>
			{@render artworks?.()}
		</div>

		<div
			id="panel-tours"
			role="tabpanel"
			aria-labelledby="tab-tours"
			class="transition-opacity duration-200"
			class:opacity-0={$activeTab !== 'tours'}
			class:hidden={$activeTab !== 'tours'}
		>
			{@render tours?.()}
		</div>
	</div>
</div>

<style>
    :global(.tab-container) {
        position: relative;
        z-index: 10;
    }

    :global(.transition-all) {
        transition-property: all;
        transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
        transition-duration: 200ms;
    }
</style>