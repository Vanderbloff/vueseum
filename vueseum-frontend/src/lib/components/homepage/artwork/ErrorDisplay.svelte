<script lang="ts">
	import { Alert, AlertDescription, AlertTitle } from "$lib/components/ui/alert";
	import { AlertCircle, AlertTriangle, XCircle } from "lucide-svelte";
	import { Button } from "$lib/components/ui/button";

	type AlertVariant = 'default' | 'destructive';

	interface ErrorDisplayProps {
		type: 'search' | 'load' | 'pagination';
		message: string;
		retryFn?: () => void;
	}

	let { type, message, retryFn }: ErrorDisplayProps = $props();

	const variants = {
		search: {
			icon: AlertCircle,
			title: "Search Error",
			variant: 'default' satisfies AlertVariant,
			iconClass: "text-amber-600"
		},
		load: {
			icon: XCircle,
			title: "Loading Error",
			variant: 'destructive' satisfies AlertVariant,
			iconClass: "text-red-600"
		},
		pagination: {
			icon: AlertTriangle,
			title: "Navigation Error",
			variant: 'default' satisfies AlertVariant,
			iconClass: "text-amber-600"
		}
	} as const;

	const CurrentIcon = $derived(variants[type].icon);
</script>

<Alert variant={variants[type].variant} class="mt-6">
	<div class="flex items-start gap-4">
		<CurrentIcon class="h-4 w-4 {variants[type].iconClass}" />
		<div class="flex-1">
			<AlertTitle class="text-sm font-medium">
				{variants[type].title}
			</AlertTitle>
			<AlertDescription class="text-sm mt-1">
				{message}
			</AlertDescription>
			{#if retryFn}
				<Button
					variant="outline"
					size="sm"
					class="mt-4"
					onclick={retryFn}
				>
					Try Again
				</Button>
			{/if}
		</div>
	</div>
</Alert>