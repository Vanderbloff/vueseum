<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { onDestroy } from 'svelte';

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

	const state = $state({
		currentUrl: null as string | null,
		isLoading: true,
		hasError: false,
		attemptedUrls: new Set<string>()
	});

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
		console.log('Attempting to load image:', {
			url,
			currentAttempts: Array.from(state.attemptedUrls)
		});

		if (!url || state.attemptedUrls.has(url)) {
			console.log('Skipping URL - null or already attempted:', url);
			return;
		}

		state.isLoading = true;
		state.hasError = false;
		const proxiedUrl = getProxiedUrl(url);

		if (!proxiedUrl) {
			console.log('Failed to generate proxy URL for:', url);
			handleImageFailure(url);
			return;
		}

		state.attemptedUrls.add(url);

		fetch(new URL(proxiedUrl, window.location.origin))
			.then(async response => {
				console.log('Fetch response:', {
					url,
					status: response.status,
					ok: response.ok
				});

				if (!response.ok) {
					throw new Error(`HTTP error! status: ${response.status}`);
				}
				return response.blob();
			})
			.then(blob => {
				console.log('Blob received:', {
					url,
					size: blob.size
				});

				if (blob.size === 0) {
					throw new Error("Empty blob received");
				}

				state.currentUrl = URL.createObjectURL(blob);
				state.isLoading = false;
			})
			.catch(error => {
				console.error('Image load failed:', {
					url,
					error: error.message,
					currentAttempts: Array.from(state.attemptedUrls)
				});
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

		if (failedUrl === primaryUrl && thumbnailUrl && !state.attemptedUrls.has(thumbnailUrl)) {
			console.log('Switching to thumbnail:', thumbnailUrl);
			tryLoadImage(thumbnailUrl);
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
	<div class="w-full h-full overflow-hidden flex items-center justify-center">
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