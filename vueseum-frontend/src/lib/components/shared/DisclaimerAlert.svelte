
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

	const state = $state({
		hasSeenDisclaimer: false,
		mounted: false
	});

	onMount(() => {
		if (browser) {
			state.hasSeenDisclaimer = localStorage.getItem('hasSeenDisclaimer') === 'true';
			state.mounted = true;
		}
	});
</script>

{#if state.mounted}
<AlertDialog open={!state.hasSeenDisclaimer}>
	<AlertDialogContent>
		<AlertDialogHeader>
			<AlertDialogTitle>Welcome to Vueseum</AlertDialogTitle>
			<AlertDialogDescription>
				Please note that Vueseum is an independent application that uses public-domain artwork data from museums' open access programs. While we strive to provide accurate information, Vueseum is not officially associated with or endorsed by any museum or art institution. The museums whose collections are featured here are not responsible for the application's content or functionality.
			</AlertDialogDescription>
		</AlertDialogHeader>
		<AlertDialogFooter>
			<AlertDialogAction onclick={() => {
					localStorage.setItem('hasSeenDisclaimer', 'true');
					state.hasSeenDisclaimer = true;
			}}>
				I understand
			</AlertDialogAction>
		</AlertDialogFooter>
	</AlertDialogContent>
</AlertDialog>
{/if}