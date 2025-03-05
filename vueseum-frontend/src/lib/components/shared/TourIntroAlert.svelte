<script lang="ts">
	import {
		AlertDialog,
		AlertDialogAction,
		AlertDialogContent,
		AlertDialogDescription,
		AlertDialogFooter,
		AlertDialogHeader,
		AlertDialogTitle
	} from '$lib/components/ui/alert-dialog';
	import { onMount } from 'svelte';
	import { browser } from '$app/environment';
	import { page } from '$app/state';

	const state = $state({
		hasSeenTourIntro: false,
		mounted: false,
		showIntro: false
	});

	onMount(() => {
		if (browser) {
			state.hasSeenTourIntro = localStorage.getItem('hasSeenTourIntro') === 'true';
			state.mounted = true;
		}
	});

	// Watch for Tours page route change to trigger the intro dialog
	$effect(() => {
		if (state.mounted && !state.hasSeenTourIntro) {
			state.showIntro = page.url.pathname === '/tours';
		}
	});

	function handleClose() {
		localStorage.setItem('hasSeenTourIntro', 'true');
		state.hasSeenTourIntro = true;
		state.showIntro = false;
	}
</script>

{#if state.mounted}
	<AlertDialog open={state.showIntro}>
		<AlertDialogContent class="sm:max-w-[600px]">
			<AlertDialogHeader>
				<AlertDialogTitle>Create Your Personalized Museum Experience</AlertDialogTitle>
				<AlertDialogDescription class="space-y-4">
					<p>
						Vueseum helps you create personalized museum experiences through curated tours.
						Discover both famous masterpieces and hidden gems tailored to your interests.
					</p>

					<div class="space-y-2 pt-2">
						<p class="font-medium text-base">How to Create a Tour:</p>
						<p>
							Click the "Generate Tour" button and customize your experience with:
						</p>
						<ul class="list-disc pl-5 text-sm space-y-1">
							<li>Number of artworks you'd like to see</li>
							<li>Specific artists or artworks you're interested in</li>
							<li>Preferred art mediums (paintings, sculptures, etc.)</li>
							<li>Cultural or geographical focus</li>
						</ul>
					</div>

					<div class="space-y-2 pt-2">
						<p class="font-medium text-base">Managing Your Tours:</p>
						<p>Each tour card in your collection provides options to:</p>
						<ul class="list-disc pl-5 text-sm space-y-1">
							<li>Update the title and description</li>
							<li>Delete tours from your collection</li>
							<li>Validate if artworks are still on display</li>
						</ul>
					</div>

					<div class="mt-3 p-3 bg-muted rounded-md text-sm text-muted-foreground">
						<p class="font-medium mb-1">Tips for Best Results:</p>
						<p>Be specific with your preferences to get the most relevant tours. The more focused your interests, the more personalized your experience will be.</p>
						<p class="mt-2">You can create up to 3 tours per day and save up to 10 tours total.</p>
					</div>
				</AlertDialogDescription>
			</AlertDialogHeader>
			<AlertDialogFooter>
				<AlertDialogAction onclick={handleClose}>
					Got it
				</AlertDialogAction>
			</AlertDialogFooter>
		</AlertDialogContent>
	</AlertDialog>
{/if}