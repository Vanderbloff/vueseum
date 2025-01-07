<!--src/lib/components/tour/TourCard.svelte-->
<script lang="ts">
	import { Clock, MapPin } from 'lucide-svelte';
	import { goto } from '$app/navigation';
	import {
		AlertDialog,
		AlertDialogAction,
		AlertDialogCancel,
		AlertDialogContent,
		AlertDialogDescription,
		AlertDialogFooter,
		AlertDialogHeader,
		AlertDialogTitle,
		AlertDialogTrigger,
	} from "$lib/components/ui/alert-dialog";
	import { Button } from "$lib/components/ui/button";
	import { Trash2 } from "lucide-svelte";

	interface Tour {
		id: number;
		name: string;
		description: string;
		theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
		estimatedDuration: number;
		museum: {
			name: string;
			location: string;
		};
	}

	let { tour, onSelect = (tour: Tour) => goto(`/tours/${tour.id}`), onDelete } = $props<{
		tour: Tour;
		onSelect?: (tour: Tour) => void;
		onDelete?: (tourId: string) => void;
	}>();

	function handleDelete() {
		if (onDelete) {
			onDelete(tour.id);
		}
	}

	function formatDuration(minutes: number): string {
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;
		return hours > 0
			? `${hours}h ${remainingMinutes}m`
			: `${remainingMinutes}m`;
	}

	// Helper to make theme more readable
	function formatTheme(theme: Tour['theme']): string {
		return theme.split('_')
			.map(word => word.charAt(0) + word.slice(1).toLowerCase())
			.join(' ');
	}
</script>

<!-- Replace the existing button with shadcn Button and add delete dialog -->
<div class="relative group">
	<Button
		variant="default"
		class="w-full h-auto p-4 justify-start hover:no-underline"
		onclick={() => onSelect(tour)}
	>
		<div class="flex flex-col gap-2 text-left w-full">
			<h3 class="text-lg font-semibold truncate">{tour.name}</h3>
			<p class="text-sm text-muted-foreground overflow-hidden whitespace-nowrap text-ellipsis">{tour.description}</p>

			<div class="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
				<div class="flex items-center gap-1">
					<Clock class="w-4 h-4" />
					<span>{formatDuration(tour.estimatedDuration)}</span>
				</div>
				<div class="flex items-center gap-1">
					<MapPin class="w-4 h-4" />
					<span>{formatTheme(tour.theme)}</span>
				</div>
			</div>

			<div class="text-sm text-muted-foreground mt-1">
				{tour.museum.name}
			</div>
		</div>
	</Button>

	<!-- Delete button and dialog -->
	<AlertDialog>
		<AlertDialogTrigger>
			<Button
				variant="ghost"
				size="icon"
				class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity"
			>
				<Trash2 class="h-4 w-4" />
			</Button>
		</AlertDialogTrigger>
		<AlertDialogContent>
			<AlertDialogHeader>
				<AlertDialogTitle>Delete Tour</AlertDialogTitle>
				<AlertDialogDescription>
					Are you sure you want to delete "{tour.name}"? This action cannot be undone.
				</AlertDialogDescription>
			</AlertDialogHeader>
			<AlertDialogFooter>
				<AlertDialogCancel>Cancel</AlertDialogCancel>
				<AlertDialogAction onclick={handleDelete}>
					Delete
				</AlertDialogAction>
			</AlertDialogFooter>
		</AlertDialogContent>
	</AlertDialog>
</div>