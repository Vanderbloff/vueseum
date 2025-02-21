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
			handleImageFailure();
			return;
		}

		state.attemptedUrls.add(url);
		const requestUrl = new URL(proxiedUrl, window.location.origin);

		fetch(requestUrl)
			.then(response => {
				console.log('Fetch response:', {
					url: url,
					status: response.status,
					contentType: response.headers.get("Content-Type")
				});

				if (!response.ok) {
					throw new Error(`HTTP error! status: ${response.status}`);
				}

				const contentType = response.headers.get("Content-Type");
				if (!contentType || !contentType.startsWith("image/")) {
					throw new Error(`Invalid content type: ${contentType}`);
				}

				return response.blob();
			})
			.then(blob => {
				console.log('Blob received:', {
					url: url,
					size: blob.size,
					type: blob.type
				});

				if (blob.size === 0) {
					throw new Error("Image blob is empty.");
				}

				state.currentUrl = URL.createObjectURL(blob);
				state.isLoading = false;
			})
			.catch(error => {
				console.error("Image load failed:", {
					url: url,
					proxiedUrl: proxiedUrl,
					error: error.message
				});
				handleImageFailure();
			});
	}

	function handleImageFailure() {
		console.log('handleImageFailure called. Current state:', {
			primaryUrl,
			thumbnailUrl,
			attemptedUrls: Array.from(state.attemptedUrls)
		});

		const hasTriedPrimary = primaryUrl && state.attemptedUrls.has(primaryUrl);
		const hasTriedThumbnail = thumbnailUrl && state.attemptedUrls.has(thumbnailUrl);

		console.log('Attempt status:', {
			hasTriedPrimary,
			hasTriedThumbnail
		});

		if (hasTriedPrimary && !hasTriedThumbnail && thumbnailUrl) {
			console.log('Primary image failed, attempting thumbnail:', thumbnailUrl);
			tryLoadImage(thumbnailUrl);
		} else {
			console.log('All attempts failed or no more URLs to try');
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