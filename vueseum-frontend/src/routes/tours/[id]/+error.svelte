<!-- src/routes/tours/[id]/+error.svelte -->
<script lang="ts">
	import { page } from '$app/state';
</script>

<div class="max-w-4xl mx-auto px-4 py-12 text-center">
	<h1 class="text-2xl font-bold mb-4">
		{page.error?.message || 'Something went wrong'}
	</h1>
	<p class="mb-6 text-gray-600">
		We're unable to load this tour at the moment.
	</p>
	<a
		href="/tours"
		class="inline-block px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
	>
		Return to Tours
	</a>
</div>