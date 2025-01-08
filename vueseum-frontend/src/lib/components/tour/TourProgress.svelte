<script lang="ts">
	import { Progress } from "$lib/components/ui/progress";
	import { Alert, AlertDescription } from "$lib/components/ui/alert";
	import { XCircle } from "lucide-svelte";

	let {
		requestId,
		onComplete
	} = $props<{
		requestId: string;
		onComplete: () => void;
	}>();

	const state = $state({
		progress: 0,
		currentTask: "Starting tour generation...",
		hasError: false,
		errorMessage: "",
		isComplete: false
	});

	$effect(() => {
		let intervalId: number;

		const checkProgress = async () => {
			try {
				const response = await fetch(`/api/v1/tours/generation/${requestId}/status`);

				if (!response.ok) {
					throw new Error('Failed to check generation progress');
				}

				const data = await response.json();

				state.progress = Math.round(data.progress * 100);
				state.currentTask = data.currentTask;
				state.hasError = data.hasError;
				state.errorMessage = data.errorMessage || 'An error occurred during tour generation';

				// If complete or error, stop polling
				if (data.progress >= 1 || data.hasError) {
					state.isComplete = true;
					clearInterval(intervalId);
					onComplete();

					if (!state.hasError) {
						onComplete();
					}
				}
			} catch (error) {
				state.hasError = true;
				state.errorMessage = error instanceof Error ? error.message : 'Failed to check generation progress';
				clearInterval(intervalId);
			}
		};

		// Start polling immediately
		checkProgress();
		intervalId = setInterval(checkProgress, 1000);

		// Cleanup interval on unmount
		return () => clearInterval(intervalId);
	});
</script>

{#if state.hasError}
	<Alert variant="destructive" class="mt-4">
		<XCircle class="h-4 w-4" />
		<AlertDescription>
			{state.errorMessage}
		</AlertDescription>
	</Alert>
{:else if !state.isComplete}
	<div class="space-y-2 mt-4">
		<Progress value={state.progress} class="w-full" />
		<p class="text-sm text-muted-foreground">
			{state.currentTask}
		</p>
	</div>
{/if}