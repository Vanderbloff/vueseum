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
		currentUrl: primaryUrl || thumbnailUrl,
		isLoading: true,
		hasError: false
	});

	function getProxiedUrl(url: string | null): string | null {
		if (!url) return null;
		try {
			const decodedUrl = decodeURIComponent(url);
			return `/api/v1/images/proxy?url=${encodeURIComponent(decodedUrl)}`;
		} catch (error) {
			console.error('URL encoding error:', error);
			return null;
		}
	}
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
			src={getProxiedUrl(state.currentUrl)}
			{alt}
			class="max-w-full max-h-full w-auto h-auto {className}"
			style="object-fit: {objectFit};"
			onerror={() => {
                // If primary URL fails, try thumbnail URL and vice versa
                if (state.currentUrl === primaryUrl && thumbnailUrl) {
                    state.currentUrl = thumbnailUrl;
                } else if (state.currentUrl === thumbnailUrl && primaryUrl) {
                    state.currentUrl = primaryUrl;
                } else {
                    state.hasError = true;
                }
            }}
			onload={() => state.isLoading = false}
		/>
	</div>
{/if}