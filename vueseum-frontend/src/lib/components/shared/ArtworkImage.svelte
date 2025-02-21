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

	function tryLoadImage(url: string | null) {
		if (!url || state.attemptedUrls.has(url)) {
			console.log('Skipping URL - null or already attempted:', url);
			return;
		}

		console.log('Attempting to load image:', url);
		state.isLoading = true;
		state.hasError = false;
		const proxiedUrl = getProxiedUrl(url);

		if (!proxiedUrl) {
			console.log('Failed to generate proxy URL for:', url);
			handleImageFailure(url);
			return;
		}

		state.attemptedUrls.add(url);
		const requestUrl = new URL(proxiedUrl, window.location.origin);

		fetch(requestUrl)
			.then(response => {
				if (!response.ok) {
					// Important: Throw error for both 404 and other error status codes
					throw new Error(`HTTP error! status: ${response.status}`);
				}
				return response.blob();
			})
			.then(blob => {
				if (blob.size === 0) {
					throw new Error("Empty blob received");
				}
				state.currentUrl = URL.createObjectURL(blob);
				state.isLoading = false;
			})
			.catch(error => {
				console.error("Image load failed:", {
					originalUrl: url,
					proxiedUrl: proxiedUrl,
					error: error.message
				});
				handleImageFailure(url);
			});
	}

	function handleImageFailure(failedUrl: string) {
		console.log('Handle image failure called for:', failedUrl);

		// Check if this was the primary URL failing
		if (failedUrl === primaryUrl && thumbnailUrl && !state.attemptedUrls.has(thumbnailUrl)) {
			console.log('Primary image failed, attempting thumbnail:', thumbnailUrl);
			tryLoadImage(thumbnailUrl);
			return;
		}

		// If we reach here, either:
		// 1. The thumbnail failed
		// 2. There is no thumbnail
		// 3. Both URLs have been tried
		console.log('No more URLs to try, showing error state');
		state.hasError = true;
		state.isLoading = false;
	}

	// Make sure to clean up object URLs when component is destroyed
	onDestroy(() => {
		if (state.currentUrl?.startsWith('blob:')) {
			URL.revokeObjectURL(state.currentUrl);
		}
	});

	$effect(() => {
		if (primaryUrl) {
			const proxiedUrl = getProxiedUrl(primaryUrl);
			console.log('Generated proxy URL:', {
				original: primaryUrl,
				proxied: proxiedUrl
			});
			tryLoadImage(primaryUrl);
		} else if (thumbnailUrl) {
			const proxiedUrl = getProxiedUrl(thumbnailUrl);
			console.log('Generated proxy URL:', {
				original: thumbnailUrl,
				proxied: proxiedUrl
			});
			tryLoadImage(thumbnailUrl);
		} else {
			state.isLoading = false;
			state.hasError = true;
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
			onerror={() => handleImageFailure()}
			onload={() => {
            console.log('Image loaded successfully:', state.currentUrl);
            state.isLoading = false;
        }}
		/>
	</div>
{/if}