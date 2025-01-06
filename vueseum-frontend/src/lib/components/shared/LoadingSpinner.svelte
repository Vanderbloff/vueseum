// src/lib/components/shared/LoadingSpinner.svelte
<script lang="ts">
	// The size prop determines how large the spinner should be
	// We'll support standard sizing options that match common UI patterns
	export let size: 'sm' | 'md' | 'lg' = 'md';

	// We'll also allow customization of the color, defaulting to a neutral gray
	export let color: string = 'text-gray-600';

	// Add an optional label for accessibility
	export let label: string = 'Loading...';

	// This function maps size names to actual CSS classes
	// We use Tailwind's width/height utilities for consistent sizing
	function getSizeClasses(size: 'sm' | 'md' | 'lg') {
		// The sizes are proportional, following a common UI sizing scale
		return {
			'sm': 'w-4 h-4',    // 16px - good for inline or small areas
			'md': 'w-8 h-8',    // 32px - default size, works in most contexts
			'lg': 'w-12 h-12'   // 48px - prominent loading states
		}[size];
	}
</script>

<div class="inline-block" role="status" aria-label={label}>
	<svg
		class={`animate-spin ${getSizeClasses(size)} ${color}`}
		xmlns="http://www.w3.org/2000/svg"
		fill="none"
		viewBox="0 0 24 24"
	>
		<!-- This path creates the spinning circle effect -->
		<!-- The stroke-dasharray and opacity create the "gap" in the circle -->
		<circle
			class="opacity-25"
			cx="12"
			cy="12"
			r="10"
			stroke="currentColor"
			stroke-width="4"
		/>
		<!-- This path creates the spinning indicator -->
		<path
			class="opacity-75"
			fill="currentColor"
			d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
		/>
	</svg>
	<span class="sr-only">{label}</span>
</div>