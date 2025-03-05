<script lang="ts">
	import { MapPin, Edit, Trash2, CheckCircle, Loader2, MoreVertical, Calendar } from 'lucide-svelte';
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
	import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '$lib/components/ui/dropdown-menu';
	import type { Tour } from '$lib/types/tour';

	let {
		tour,
		onSelect = (tour: Tour) => goto(`/tours/${tour.id}`),
		onDelete,
		onEdit,
		onValidate,
		isUpdating = false,
		isValidating = false
	} = $props<{
		tour: Tour;
		onSelect?: (tour: Tour) => void;
		onDelete?: (tourId: number) => void;
		onEdit?: (tourId: number, updates: { name: string; description: string }) => Promise<boolean>;
		onValidate?: (tourId: number) => Promise<void>;
		isUpdating?: boolean;
		isValidating?: boolean;
	}>();

	const state = $state({
		editName: tour.name,
		editDescription: tour.description,
		errors: {
			name: '',
			description: ''
		},
		isValid: true,
		isDialogOpen: false,
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

	function formatCreationDate(dateString?: string): string {
		if (!dateString) return 'Unknown date';

		const date = new Date(dateString);
		const now = new Date();
		const diffTime = Math.abs(now.getTime() - date.getTime());
		const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

		if (diffDays < 1) return 'Today';
		if (diffDays === 1) return 'Yesterday';
		if (diffDays < 7) return `${diffDays} days ago`;

		return date.toLocaleDateString();
	}
</script>

<div class="relative group">
	<Button
		variant="outline"
		class="w-full h-auto p-4 justify-start hover:no-underline bg-card text-card-foreground"
		onclick={() => onSelect(tour)}
	>
		<div class="flex flex-col gap-2 text-left w-full">
			<h3 class="text-lg font-semibold truncate pr-16 text-muted-foreground">{tour.name}</h3>
			<p class="text-sm text-muted-foreground overflow-hidden whitespace-nowrap text-ellipsis">
				{tour.description}
			</p>

			<div class="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
				<div class="flex items-center gap-1">
					<MapPin class="w-4 h-4" />
					<span>{formatTheme(tour.theme)}</span>
				</div>
				<span>•</span>
				<span>{tour.stops.length} stops</span>
			</div>

			<div class="text-sm text-muted-foreground mt-1">
				{tour.museum.name}
			</div>
			{#if tour.createdAt}
				<div class="flex items-center gap-1">
					<Calendar class="w-3 h-3" />
					<span>Created {formatCreationDate(tour.createdAt)}</span>
				</div>
			{/if}
		</div>
	</Button>

	<!-- Validation Results Section -->
	{#if isValidating}
		<div class="mt-2 px-4 py-2 bg-muted/50 rounded-md">
			<div class="flex items-center space-x-2">
				<Loader2 class="h-4 w-4 animate-spin text-muted-foreground" />
				<p class="text-sm text-muted-foreground">Checking artwork availability...</p>
			</div>
		</div>
	{:else if tour.lastValidated}
		{#if (tour.unavailableArtworks?.length ?? 0) > 0}
			<div class="mt-2 px-4 py-2 bg-destructive/10 rounded-md">
				<p class="text-sm text-destructive">
					{tour.unavailableArtworks.length} artwork{tour.unavailableArtworks.length === 1 ? '' : 's'}
					{tour.unavailableArtworks.length === 1 ? 'is' : 'are'} no longer on display
				</p>
				<div class="mt-1 text-xs text-muted-foreground">
					{#each tour.unavailableArtworks as artwork}
						<p>{artwork.title} (Gallery {artwork.galleryNumber})</p>
					{/each}
				</div>
				<div class="mt-2 text-xs text-muted-foreground">
					Last checked: {new Date(tour.lastValidated ?? '').toLocaleString()}
				</div>
			</div>
		{:else}
			<div class="mt-2 px-4 py-2 bg-green-100 rounded-md">
				<p class="text-sm text-green-800">
					All artworks in this tour are currently on display.
				</p>
				<div class="mt-2 text-xs text-muted-foreground">
					Last checked: {new Date(tour.lastValidated ?? '').toLocaleString()}
				</div>
			</div>
		{/if}
	{/if}

	<!-- Action dropdown menu -->
	<div class="absolute top-2 right-2">
		<DropdownMenu>
			<DropdownMenuTrigger>
				<Button variant="ghost" size="icon" class="h-8 w-8">
					<MoreVertical class="h-4 w-4" />
				</Button>
			</DropdownMenuTrigger>
			<DropdownMenuContent align="end">
				<DropdownMenuItem
					disabled={isValidating}
					onclick={() => onValidate?.(tour.id)}
				>
					<div class="flex items-center">
						{#if isValidating}
							<Loader2 class="h-4 w-4 mr-2 animate-spin" />
						{:else}
							<CheckCircle class="h-4 w-4 mr-2" />
						{/if}
						Validate Tour
					</div>
				</DropdownMenuItem>

				<DropdownMenuItem
					disabled={isUpdating}
					onclick={() => state.isDialogOpen = true}
				>
					<Edit class="h-4 w-4 mr-2" />
					Edit Tour
				</DropdownMenuItem>

				<DropdownMenuItem
					onclick={() => document.getElementById(`delete-dialog-${tour.id}`)?.click()}
				>
					<Trash2 class="h-4 w-4 mr-2" />
					Delete Tour
				</DropdownMenuItem>
			</DropdownMenuContent>
		</DropdownMenu>
	</div>

	<!-- Edit dialog -->
	<AlertDialog bind:open={state.isDialogOpen}>
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

	<!-- Delete dialog with hidden trigger -->
	<AlertDialog>
		<AlertDialogTrigger class="hidden" id={`delete-dialog-${tour.id}`}>
			Delete
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