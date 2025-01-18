<script lang="ts">
	import { Button } from "$lib/components/ui/button";
	import { Sun, Moon } from "lucide-svelte";
	import { theme } from './ThemeProvider.svelte'

	function toggleTheme() {
		theme.update(current => current === 'dark' ? 'light' : 'dark');
	}
</script>

<Button
	variant="ghost"
	size="icon"
	onclick={toggleTheme}
	aria-label="Toggle theme"
>
	{#if $theme === 'dark'}
		<Sun class="h-5 w-5" />
	{:else}
		<Moon class="h-5 w-5" />
	{/if}
</Button>