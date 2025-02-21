<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { onDestroy, onMount } from 'svelte';

	console.warn('Component initialized 2');

	onMount(() => {
		console.log('ArtworkImage mounted');
	});

	$effect.root(() => {
		console.log('Root effect running');
	});

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

	console.log('ArtworkImage component initializing with props:', {
		primaryUrl,
		thumbnailUrl,
		alt,
		className,
		objectFit
	});

	const state = $state({
		componentDidInitialize: false,  // New state property
		currentUrl: null as string | null,
		isLoading: true,
		hasError: false,
		attemptedUrls: new Set<string>()
	});

	// At component initialization
	state.componentDidInitialize = true;

	console.log('Initial state created:', state);

	function getProxiedUrl(url: string | null): string | null {
		if (!url) return null;

		try {
			// Clean encode the URL to handle special characters
			const encodedUrl = encodeURIComponent(url);
			return `/api/v1/images/proxy?url=${encodedUrl}`;
		} catch (error) {
			console.error('Error encoding URL:', url, error);
			return null;
		}
	}

	// At component initialization
	$effect(() => {
		console.log('Initial URLs:', {
			primaryUrl,
			thumbnailUrl,
			attemptedUrls: Array.from(state.attemptedUrls)
		});

		if (primaryUrl) {
			tryLoadImage(primaryUrl);
		} else if (thumbnailUrl) {
			tryLoadImage(thumbnailUrl);
		} else {
			state.isLoading = false;
			state.hasError = true;
		}
	});

	function tryLoadImage(url: string | null) {
		console.log('ENTRY: tryLoadImage');

		if (!url || state.attemptedUrls.has(url)) {
			console.log('EXIT EARLY: null or attempted URL');
			return;
		}

		state.isLoading = true;
		state.hasError = false;
		console.log('CHECKPOINT 1: After state updates');

		const proxiedUrl = getProxiedUrl(url);
		console.log('CHECKPOINT 2: After getProxiedUrl', { proxiedUrl });

		if (!proxiedUrl) {
			console.log('EXIT: Failed to generate proxy URL');
			handleImageFailure(url);
			return;
		}

		state.attemptedUrls.add(url);
		console.log('CHECKPOINT 3: Added to attemptedUrls');

		fetch(proxiedUrl)
			.then(response => {
				console.log('CHECKPOINT 4: Fetch response received');
				if (!response.ok) {
					throw new Error(`HTTP error! status: ${response.status}`);
				}
				return response.blob();
			})
			.then(blob => {
				console.log('CHECKPOINT 5: Blob received');
				if (blob.size === 0) {
					throw new Error("Empty blob received");
				}
				state.currentUrl = URL.createObjectURL(blob);
				state.isLoading = false;
			})
			.catch(error => {
				console.error('Image load failed:', error.message);
				handleImageFailure(url);
			});
	}

	function handleImageFailure(failedUrl: string) {
		console.log('Handle image failure called:', {
			failedUrl,
			primaryUrl,
			thumbnailUrl,
			attemptedUrls: Array.from(state.attemptedUrls)
		});

		// Check if this was the primary URL failing
		if (failedUrl === primaryUrl && thumbnailUrl) {
			if (!state.attemptedUrls.has(thumbnailUrl)) {
				console.log('Primary failed, attempting thumbnail:', thumbnailUrl);
				tryLoadImage(thumbnailUrl);
			} else {
				console.log('Thumbnail already attempted, showing error state');
				state.hasError = true;
				state.isLoading = false;
			}
		} else {
			console.log('No more URLs to try, showing error state');
			state.hasError = true;
			state.isLoading = false;
		}
	}

	// Make sure to clean up object URLs when component is destroyed
	onDestroy(() => {
		if (state.currentUrl?.startsWith('blob:')) {
			URL.revokeObjectURL(state.currentUrl);
		}
	});

</script>

{#if state.isLoading}
	<Skeleton class="w-full h-full" />
{:else if state.hasError || !state.currentUrl}
	<div class="w-full h-full bg-muted flex items-center justify-center">
		<span class="text-muted-foreground">Image unavailable</span>
	</div>
{:else}
	<div class="w-full h-full overflow-hidden flex items-center justify-center"
			 data-initialized={state.componentDidInitialize.toString()}
	>
		<img
			src={state.currentUrl}
			{alt}
			class="max-w-full max-h-full w-auto h-auto {className}"
			style="object-fit: {objectFit};"
			onerror={() => handleImageFailure(primaryUrl)}
			onload={() => {
            console.log('Image loaded successfully:', state.currentUrl);
            state.isLoading = false;
        }}
		/>
	</div>
{/if}