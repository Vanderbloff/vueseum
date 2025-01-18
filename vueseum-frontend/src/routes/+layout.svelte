<script lang="ts">
	import '../app.css';
	import ThemeProvider from '$lib/components/theme/ThemeProvider.svelte';
	import ThemeToggle from '$lib/components/theme/ThemeToggle.svelte';
	import { Button } from "$lib/components/ui/button";
	import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "$lib/components/ui/dialog";
	let { children } = $props();
</script>

<ThemeProvider>
	<div class="min-h-screen bg-background">
		<header class="w-full border-b bg-background/50 backdrop-blur-sm fixed top-0 z-10">
			<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div class="flex items-center justify-between h-16">
					<h1 class="text-2xl font-bold tracking-tight text-foreground">Vueseum</h1>

					<div class="flex items-center gap-4">
						<Dialog>
							<DialogTrigger>
								<Button variant="ghost" class="text-foreground">About</Button>
							</DialogTrigger>
							<DialogContent>
								<DialogHeader>
									<DialogTitle>About Vueseum</DialogTitle>
									<DialogDescription>
										<div class="space-y-4">
											<p>
												Vueseum is a virtual museum experience that allows you to explore
												artworks and create personalized tours from various museum collections.
											</p>
											<p>
												Browse our curated selection of artworks or generate a custom tour
												based on your interests and preferences. Each tour is uniquely crafted
												to provide an engaging and educational experience.
											</p>
										</div>
									</DialogDescription>
								</DialogHeader>
							</DialogContent>
						</Dialog>

						<ThemeToggle />
					</div>
				</div>
			</div>
		</header>

		<main class="pt-16">
			{@render children()}
		</main>
	</div>
</ThemeProvider>