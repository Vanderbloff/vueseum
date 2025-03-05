<script lang="ts">
	import '../app.css';
	import ThemeProvider from '$lib/components/theme/ThemeProvider.svelte';
	import ThemeToggle from '$lib/components/theme/ThemeToggle.svelte';
	import { Button } from "$lib/components/ui/button";
	import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "$lib/components/ui/dialog";
	import { MoreVertical } from 'lucide-svelte';
	import {
		DropdownMenu,
		DropdownMenuContent,
		DropdownMenuItem,
		DropdownMenuTrigger,
		DropdownMenuSeparator
	} from "$lib/components/ui/dropdown-menu";

	import DisclaimerAlert from '$lib/components/shared/DisclaimerAlert.svelte';
	import { browser } from '$app/environment';
	import TourIntroAlert from '$lib/components/shared/TourIntroAlert.svelte';

	let { children } = $props();
	const state = $state({
		hasSeenDisclaimer: false
	});

	$effect(() => {
		if (typeof window !== 'undefined') {
			state.hasSeenDisclaimer = localStorage.getItem('hasSeenDisclaimer') === 'true';
		}
	});

	$effect(() => {
		if (browser) {
			const splash = document.getElementById('vueseum-splash');
			if (splash) {
				splash.style.opacity = '0';
				setTimeout(() => {
					if (splash.parentNode) {
						splash.parentNode.removeChild(splash);
					}
				}, 400);
			}
			document.body.classList.add('app-ready');
		}
	});
</script>

<ThemeProvider>
	<DisclaimerAlert />
	<TourIntroAlert />
	<div class="min-h-screen bg-background">
		<header class="w-full border-b bg-background/50 backdrop-blur-sm fixed top-0 z-10">
			<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div class="flex items-center justify-between h-16">
					<a href="/" class="text-2xl font-bold tracking-tight text-foreground hover:opacity-80 transition-opacity">Vueseum</a>

					<div class="flex items-center gap-4">
						<DropdownMenu>
							<DropdownMenuTrigger>
								<Button variant="ghost" size="icon">
									<MoreVertical class="h-5 w-5" />
								</Button>
							</DropdownMenuTrigger>
							<DropdownMenuContent align="end">
								<div>
									<Dialog>
										<DialogTrigger>
											<DropdownMenuItem onSelect={(e) => e.preventDefault()}>
												Current Museums
											</DropdownMenuItem>
										</DialogTrigger>
										<DialogContent class="sm:max-w-[600px]">
											<DialogHeader>
												<DialogTitle>Current Museums</DialogTitle>
											</DialogHeader>
											<div class="space-y-4 py-4 max-h-[70vh] overflow-y-auto">
												<p class="text-muted-foreground">
													<span class="font-medium text-foreground">Metropolitan Museum of Art:</span> Explore over 400,000 artworks from the Met's collection. All artwork data is sourced from the Met's Open Access program, providing comprehensive information about pieces spanning over 5,000 years of human creativity.
												</p>

												<p class="text-muted-foreground">
													More museums will be added in future updates to expand the available collection and provide even more diverse touring options.
												</p>
											</div>
										</DialogContent>
									</Dialog>
								</div>
								<DropdownMenuSeparator />
								<div>
									<Dialog>
										<DialogTrigger>
											<DropdownMenuItem onSelect={(e) => e.preventDefault()}>
												How to Use
											</DropdownMenuItem>
										</DialogTrigger>
										<DialogContent class="sm:max-w-[600px]">
											<DialogHeader>
												<DialogTitle>How to Use Vueseum</DialogTitle>
											</DialogHeader>
											<div class="space-y-4 py-4 max-h-[70vh] overflow-y-auto">
												<p class="text-muted-foreground">
													<span class="font-medium text-foreground">Searching for Artworks:</span> Use the search bar to explore artworks by title, artist, or medium. Refine your results using filters for time periods, cultures, and departments. Each artwork card shows key information, and clicking on it reveals more details. <em><strong>Note: If you're typing or removing text within the search bar, you need to click on the search icon to refresh the results.</strong></em>
												</p>

												<p class="text-muted-foreground">
													<span class="font-medium text-foreground">Generating Tours:</span> Click the "Generate Tour" button to start creating your personalized tour. You can customize your experience by specifying your preferences such as the number of artworks you'd like to see, specific artists or artworks you're interested in, art mediums like paintings or sculptures, and any particular cultural or geographical focus.
												</p>

												<p class="text-muted-foreground">
													<span class="font-medium text-foreground">Using Your Tour:</span> Once generated, your tour will show a curated list of artworks with their locations in the museum. Each stop includes artwork details and historical context, as well as gallery locations to help guide your visit.
												</p>

												<p class="text-muted-foreground">
													<span class="font-medium text-foreground">Additional Features:</span> Use the theme toggle in the top right to switch between light and dark modes. The dropdown menu (three dots) provides access to information about participating museums and more details about Vueseum.
												</p>

												<div class="space-y-2">
													<p class="text-muted-foreground">
														<span class="font-medium text-foreground">Tips:</span>
													</p>
													<ul class="list-disc ml-6 space-y-1 text-muted-foreground">
														<li>Generate multiple tours to explore different themes</li>
														<li>Bookmark interesting tours for future visits</li>
														<li>Use filters to focus on specific interests</li>
													</ul>
												</div>
											</div>
										</DialogContent>
									</Dialog>
								</div>
								<div>
									<Dialog>
										<DialogTrigger>
											<DropdownMenuItem onSelect={(e) => e.preventDefault()}>
												About
											</DropdownMenuItem>
										</DialogTrigger>
										<DialogContent class="sm:max-w-[600px]">
											<DialogHeader>
												<DialogTitle>About Vueseum</DialogTitle>
											</DialogHeader>
											<div class="space-y-4 py-4 max-h-[70vh] overflow-y-auto">
												<p class="text-muted-foreground">
													If you've ever visited a museum with a moderately large collection, you're familiar with this challenge: with so many incredible artworks and limited time, how do you decide what to see? While guided tours help by providing structure and insight, they're not always available when you need them - maybe it's late in the day, or perhaps you're interested in a specific collection that isn't part of the standard tours.
												</p>
												<p class="text-muted-foreground">
													That's where Vueseum comes in. Whether you have just an hour before closing, want to dive deep into French ceramics for your research, or simply wish to plan an impressive date, Vueseum puts personalized, self-guided tours at your fingertips. You set the preferences, and we'll help craft an experience that matches your interests and schedule.
												</p>
												<p class="text-muted-foreground">
													This idea grew from my years as a museum volunteer. I'd often hear visitors ask "What do you recommend seeing?" or "Where can I find [insert specific types of art]?" While I could point them to popular exhibitions or general sections, I realized there could be a use for something more tailored - a tool that could help visitors shape their own museum experience, especially when traditional tours weren't an option.
												</p>
												<p class="text-muted-foreground">
													While Vueseum isn't meant to replace traditional guided tours (nothing beats the nuanced knowledge of an experienced guide), it serves as a valuable companion for those times when you want to explore on your own terms. Beyond generating tours, it also functions as a search engine for artworks across participating museums, helping you discover and learn about specific pieces that interest you.
												</p>
											</div>
										</DialogContent>
									</Dialog>
								</div>
							</DropdownMenuContent>
						</DropdownMenu>

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