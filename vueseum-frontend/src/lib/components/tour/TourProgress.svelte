<script lang="ts">
	import { Progress } from "$lib/components/ui/progress";
	import { Alert, AlertDescription } from "$lib/components/ui/alert";
	import { XCircle } from "lucide-svelte";
	import { tourApi } from '$lib/api/tour';

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
		tourApi.monitorTourProgress(
			requestId,
			(progress, task) => {
				state.progress = progress;
				state.currentTask = task;

				if (progress >= 1) {
					state.isComplete = true;
					onComplete();
				}
			}
		).catch(error => {
			state.hasError = true;
			state.errorMessage = error.message;
			state.isComplete = true;
		});
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