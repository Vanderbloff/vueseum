<script lang="ts">
	let {
		primaryUrl,
		thumbnailUrl,
		alt,
		className = '',
		objectFit = 'contain'
	} = $props<{
		primaryUrl: string | null;
		thumbnailUrl: string | null;
		alt: string;
		className?: string;
		objectFit?: 'contain' | 'cover';
	}>();

	console.log('Component props:', { primaryUrl, thumbnailUrl, alt });

	const state = $state({
		currentUrl: null as string | null,
		isLoading: true,
		hasError: false
	});

	function handleImageError() {
		console.log('Image error occurred');
		state.hasError = true;
		state.isLoading = false;
	}
</script>

<div class="w-full h-full">
	{#if state.isLoading}
		<div class="w-full h-full bg-muted animate-pulse"></div>
	{:else if state.hasError}
		<div class="w-full h-full bg-muted flex items-center justify-center">
			<span class="text-muted-foreground">Image unavailable</span>
		</div>
	{:else}
		<img
			src={primaryUrl ?? thumbnailUrl ?? ''}
			{alt}
			class="max-w-full max-h-full w-auto h-auto {className}"
			style="object-fit: {objectFit};"
			onerror={handleImageError}
			onload={() => {
                console.log('Image loaded successfully');
                state.isLoading = false;
            }}
		/>
	{/if}
</div>