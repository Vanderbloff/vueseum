<script lang="ts">
	import { MapPin, Edit, Trash2 } from 'lucide-svelte';
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
	import { Textarea } from "$lib/components/ui/textarea";

	interface Tour {
		id: number;
		name: string;
		description: string;
		theme: 'CHRONOLOGICAL' | 'ARTIST_FOCUSED' | 'CULTURAL';
		museum: {
			name: string;
			location: string;
		};
	}

	let {
		tour,
		onSelect = (tour: Tour) => goto(`/tours/${tour.id}`),
		onDelete,
		onEdit,
		isUpdating = false
	} = $props<{
		tour: Tour;
		onSelect?: (tour: Tour) => void;
		onDelete?: (tourId: number) => void;
		onEdit?: (tourId: number, updates: { name: string; description: string }) => Promise<boolean>;
		isUpdating?: boolean;
	}>();

	const state = $state({
		editName: tour.name,
		editDescription: tour.description,
		errors: {
			name: '',
			description: ''
		},
		isValid: true,
		isDialogOpen: false
	});

	function validateForm() {
		state.errors.name = '';
		state.errors.description = '';
		state.isValid = true;

		if (!state.editName.trim()) {
			state.errors.name = 'Tour name is required';
			state.isValid = false;
		}
		if (!state.editDescription.trim()) {
			state.errors.description = 'Tour description is required';
			state.isValid = false;
		}

		return state.isValid;
	}

	function handleDelete() {
		if (onDelete) {
			onDelete(tour.id);
		}
	}

	function formatTheme(theme: Tour['theme']): string {
		return theme.split('_')
			.map(word => word.charAt(0) + word.slice(1).toLowerCase())
			.join(' ');
	}
</script>

<div class="relative group">
	<Button
		variant="outline"
		class="w-full h-auto p-4 justify-start hover:no-underline bg-white"
		onclick={() => onSelect(tour)}
	>
		<div class="flex flex-col gap-2 text-left w-full">
			<h3 class="text-lg font-semibold truncate pr-16">{tour.name}</h3>
			<p class="text-sm text-muted-foreground overflow-hidden whitespace-nowrap text-ellipsis">
				{tour.description}
			</p>

			<div class="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
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

	<!-- Action buttons -->
	<div class="absolute top-2 right-2 space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
		<!-- Edit button -->
		<AlertDialog bind:open={state.isDialogOpen}>
			<AlertDialogTrigger disabled={isUpdating}>
				<Button
					variant="secondary"
					size="icon"
					class="h-8 w-8"
					disabled={isUpdating}
				>
					<Edit class="h-4 w-4" />
				</Button>
			</AlertDialogTrigger>
			<AlertDialogContent>
				<AlertDialogHeader>
					<AlertDialogTitle>Edit Tour</AlertDialogTitle>
					<AlertDialogDescription>
						Update the tour details below.
					</AlertDialogDescription>
				</AlertDialogHeader>
				<div class="space-y-4 py-4">
					<div class="space-y-2">
						<label for="name" class="text-sm font-medium">Name</label>
						<input
							type="text"
							id="name"
							bind:value={state.editName}
							class="w-full p-2 border rounded-md {state.errors.name ? 'border-red-500' : ''}"
						/>
						{#if state.errors.name}
							<p class="text-sm text-red-500">{state.errors.name}</p>
						{/if}
					</div>
					<div class="space-y-2">
						<label for="description" class="text-sm font-medium">Description</label>
						<Textarea
							id="description"
							bind:value={state.editDescription}
							class={state.errors.description ? 'border-red-500' : ''}
							rows={3}
						/>
						{#if state.errors.description}
							<p class="text-sm text-red-500">{state.errors.description}</p>
						{/if}
					</div>
				</div>
				<AlertDialogFooter>
					<AlertDialogCancel onclick={() => {
                        state.editName = tour.name;
                        state.editDescription = tour.description;
                        state.errors.name = '';
                        state.errors.description = '';
                    }}>
						Cancel
					</AlertDialogCancel>
					<AlertDialogAction
						onclick={async () => {
                            if (!validateForm() || !onEdit) return;

                            try {
                                await onEdit(tour.id, {
                                    name: state.editName,
                                    description: state.editDescription
                                });
                                state.isDialogOpen = false;
                            // eslint-disable-next-line @typescript-eslint/no-unused-vars
                            } catch (error) {
                                // Error will be handled by parent component
                            }
                        }}
						disabled={isUpdating}
					>
						{isUpdating ? 'Saving...' : 'Save Changes'}
					</AlertDialogAction>
				</AlertDialogFooter>
			</AlertDialogContent>
		</AlertDialog>

		<!-- Delete button -->
		<AlertDialog>
			<AlertDialogTrigger>
				<Button
					variant="secondary"
					size="icon"
					class="h-8 w-8"
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
</div>