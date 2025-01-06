<!-- src/lib/components/homepage/artwork/ArtworkFilters.svelte -->
<script lang="ts">
	// We'll define a type for our filters
	export interface ArtworkFilters {
		searchTerm: string;
		searchField: 'all' | 'title' | 'artist' | 'medium';
		objectType: string;
		location: string;
		era: string;
		department: string;
		onDisplay: boolean;
		hasImage: boolean;
	}
	let { onSearch } = $props<{
		onSearch: (filters: ArtworkFilters) => void;
	}>();

	const state = $state({
		filters: {
			searchTerm: '',
			searchField: 'all',
			objectType: '',
			location: '',
			era: '',
			department: '',
			onDisplay: false,
			hasImage: true
		} as ArtworkFilters
	});
</script>

<div class="space-y-6">
	<!-- Main Search Bar -->
	<div class="flex gap-2">
		<select
			class="px-3 py-2 border rounded-lg w-36"
			bind:value={state.filters.searchField}
		>
			<option value="all">All Fields</option>
			<option value="title">Title</option>
			<option value="artist">Artist</option>
			<option value="medium">Medium</option>
		</select>

		<div class="flex-1 relative">
			<input
				type="text"
				placeholder="Search all fields"
				class="w-full px-4 py-2 border rounded-lg pr-10"
				bind:value={state.filters.searchTerm}
			/>
			<button
				class="absolute right-2 top-1/2 -translate-y-1/2 p-2"
				onclick={() => onSearch(state.filters)}
			>
				🔍
			</button>
		</div>
	</div>

	<!-- Filter Section -->
	<div>
		<h3 class="font-medium mb-3">Filter By</h3>
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
			<select
				class="px-3 py-2 border rounded-lg w-full"
				bind:value={state.filters.objectType}
			>
				<option value="">Object Type / Material</option>
				<option value="painting">Painting</option>
				<option value="sculpture">Sculpture</option>
				<option value="photograph">Photograph</option>
			</select>

			<select
				class="px-3 py-2 border rounded-lg w-full"
				bind:value={state.filters.location}
			>
				<option value="">Geographic Location</option>
				<option value="europe">Europe</option>
				<option value="asia">Asia</option>
				<option value="americas">Americas</option>
			</select>

			<select
				class="px-3 py-2 border rounded-lg w-full"
				bind:value={state.filters.era}
			>
				<option value="">Date / Era</option>
				<option value="ancient">Ancient</option>
				<option value="medieval">Medieval</option>
				<option value="modern">Modern</option>
			</select>

			<select
				class="px-3 py-2 border rounded-lg w-full"
				bind:value={state.filters.department}
			>
				<option value="">Department</option>
				<option value="paintings">Paintings</option>
				<option value="sculpture">Sculpture</option>
				<option value="photographs">Photographs</option>
			</select>
		</div>
	</div>

	<!-- Checkbox Filters -->
	<div class="flex flex-wrap gap-6">
		<label class="flex items-center gap-2">
			<input
				type="checkbox"
				bind:checked={state.filters.onDisplay}
			/>
			<span>On view</span>
		</label>

		<label class="flex items-center gap-2">
			<input
				type="checkbox"
				bind:checked={state.filters.hasImage}
			/>
			<span>Has image</span>
		</label>
	</div>
</div>