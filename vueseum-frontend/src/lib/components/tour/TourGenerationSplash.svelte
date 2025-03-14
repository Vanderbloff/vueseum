<script lang="ts">
	const { stage, progress, currentStop, totalStops } = $props<{
		stage: 'selecting' | 'describing' | 'finalizing' | 'complete' | null;
		progress: number;
		currentStop?: number;
		totalStops?: number;
	}>();

	const timeMessage = $derived(
		progress < 20 ? 'This may take 2-5 minutes in total' :
			progress < 60 ? 'About 1-3 minutes remaining' :
				'Almost done'
	);
</script>

<div class="fixed inset-0 bg-background/95 backdrop-blur-sm flex justify-center items-center z-50">
	<div class="text-center p-6 max-w-md">
		<h2 class="text-2xl font-bold mb-4">Creating Your Tour</h2>

		<div class="flex justify-between mb-2 text-sm text-muted-foreground">
            <span class={stage === 'selecting' ? 'font-medium text-primary' : ''}>
                Selecting
            </span>
			<span class={stage === 'describing' ? 'font-medium text-primary' : ''}>
                Describing
            </span>
			<span class={stage === 'finalizing' ? 'font-medium text-primary' : ''}>
                Finalizing
            </span>
		</div>

		<div class="w-full bg-secondary rounded-full h-2.5 mb-4">
			<div class="bg-primary h-2.5 rounded-full" style="width: {progress}%"></div>
		</div>

		<p class="mb-4 text-muted-foreground">
			{#if stage === 'selecting'}
				Finding the perfect artworks based on your preferences...
			{:else if stage === 'describing'}
				{#if currentStop !== undefined && totalStops !== undefined}
					Creating description for stop {currentStop + 1} of {totalStops}...
				{:else}
					Creating engaging descriptions for your tour stops...
				{/if}
			{:else if stage === 'finalizing'}
				Putting the finishing touches on your personalized tour...
			{:else}
				Preparing your tour...
			{/if}
		</p>

		<div class="bg-muted p-4 rounded-lg mb-6 text-sm">
			<p class="font-medium mb-2">{timeMessage}</p>
			<p class="text-amber-600 dark:text-amber-400 font-medium mb-2">Please do not refresh this page</p>
			<p class="text-muted-foreground">Your tour is being generated with AI-powered descriptions and selections. The application is working normally even if it seems to pause occasionally.</p>
		</div>

		<div class="pulse-animation text-3xl">🎨</div>
	</div>
</div>

<style>
    .pulse-animation {
        animation: pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
    }

    @keyframes pulse {
        0%, 100% {
            opacity: 1;
        }
        50% {
            opacity: 0.5;
        }
    }
</style>