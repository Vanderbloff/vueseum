<script lang="ts">
	import { Card, CardContent } from "$lib/components/ui/card";
	import type { PageData } from './$types';

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isLoading: false,
		error: null as string | null
	});

	console.log('Page mounted'); // Debug log
</script>

<main class="container mx-auto p-4">
	<Card>
		<CardContent>
			<h1 class="text-2xl font-bold">Debug Mode</h1>
			<p class="mt-4">Basic page load test</p>
			{#if state.isLoading}
				<p>Loading...</p>
			{/if}
			{#if state.error}
				<p>Error: {state.error}</p>
			{/if}
		</CardContent>
	</Card>
</main>