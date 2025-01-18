<script lang="ts" context="module">
	// Export the store in a context module script
	import { writable } from 'svelte/store';
	export const theme = writable<'light' | 'dark'>('light');
</script>

<script lang="ts">
	import { onMount } from 'svelte';

	// Check for system preference and localStorage on mount
	onMount(() => {
		const stored = localStorage.getItem('theme');
		if (stored === 'dark' || stored === 'light') {
			theme.set(stored);
			document.documentElement.classList.toggle('dark', stored === 'dark');
		} else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
			theme.set('dark');
			document.documentElement.classList.add('dark');
		}
	});

	// Subscribe to theme changes
	theme.subscribe((value) => {
		if (typeof window !== 'undefined') {
			localStorage.setItem('theme', value);
			document.documentElement.classList.toggle('dark', value === 'dark');
		}
	});
</script>

<slot />