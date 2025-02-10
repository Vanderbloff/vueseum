<script lang="ts">
	import '../app.css';
	import ThemeProvider from '$lib/components/theme/ThemeProvider.svelte';

	let { children } = $props();
	console.log('Layout mounted'); // Debug log
</script>

<ThemeProvider>
	<div class="min-h-screen bg-background">
		<header class="w-full border-b bg-background/50 backdrop-blur-sm fixed top-0 z-10">
			<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div class="flex items-center justify-between h-16">
					<h1 class="text-2xl font-bold">Vueseum (Debug)</h1>
				</div>
			</div>
		</header>

		<main class="pt-16">
			{@render children()}
		</main>
	</div>
</ThemeProvider>