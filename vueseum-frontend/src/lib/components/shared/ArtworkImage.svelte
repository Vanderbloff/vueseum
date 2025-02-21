<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';

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
			return;
		}

		state.isLoading = true;
		state.hasError = false;
		state.currentUrl = getProxiedUrl(url);
		state.attemptedUrls.add(url);

		console.log('Attempting to load image:', {
			original: url,
			proxied: state.currentUrl
		});
	}

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
			onerror={() => {
                console.error('Image load failed:', {
                    current: state.currentUrl,
                    attempted: Array.from(state.attemptedUrls)
                });

                // If primary URL failed, try thumbnail
                if (primaryUrl && !state.attemptedUrls.has(thumbnailUrl || '')) {
                    tryLoadImage(thumbnailUrl);
                } else {
                    state.hasError = true;
                    state.isLoading = false;
                }
            }}
			onload={() => {
                console.log('Image loaded successfully:', state.currentUrl);
                state.isLoading = false;
            }}
		/>
	</div>
{/if}