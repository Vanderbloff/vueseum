<script lang="ts">
	import { page } from '$app/state';
	import { Button } from "$lib/components/ui/button";
	import { ArrowLeft } from 'lucide-svelte';

	const errorMessage = $derived(() => {
		if (page.error?.message) {
			return page.error.message;
		}

		if (page.status === 404) {
			return 'Tour not found';
		}
		return 'Something went wrong while loading this tour';
	});
</script>

<div class="max-w-4xl mx-auto px-4 py-12">
	<div class="mb-6">
		<Button
			variant="ghost"
			class="gap-2 text-foreground"
			onclick={() => window.history.back()}
		>
			<ArrowLeft class="h-5 w-5" />
			Back
		</Button>
	</div>

	<div class="text-center">
		<h1 class="text-2xl font-bold mb-4">
			{errorMessage}
		</h1>
		<p class="mb-6 text-muted-foreground">
			We're unable to load this tour at the moment. You can try again or return to your tours.
		</p>
		<div class="space-x-4">
			<Button
				variant="outline"
				onclick={() => window.location.reload()}
			>
				Try Again
			</Button>
			<Button
				onclick={() => window.location.href = '/?tab=tours'}
			>
				Return to Tours
			</Button>
		</div>
	</div>
</div>