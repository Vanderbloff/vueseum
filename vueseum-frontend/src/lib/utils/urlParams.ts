// src/lib/utils/urlParams.ts
export function updateUrlParams(params: Record<string, string | string[] | boolean | null>) {
	const url = new URL(window.location.href);

	const tab = url.searchParams.get('tab');
	url.search = '';
	if (tab) url.searchParams.set('tab', tab);

	// Add non-null params with proper array handling
	Object.entries(params).forEach(([key, value]) => {
		if (value !== null && value !== undefined) {
			if (Array.isArray(value)) {
				// Only add non-empty array values
				value.filter(v => v).forEach(v =>
					url.searchParams.append(key, v));
			} else {
				url.searchParams.set(key, value.toString());
			}
		}
	});

	// Update URL without page reload
	window.history.replaceState({}, '', url.toString());
}

export function getUrlParams(): Record<string, string[]> {
	const url = new URL(window.location.href);
	const params: Record<string, string[]> = {};

	url.searchParams.forEach((value, key) => {
		if (!params[key]) params[key] = [];
		params[key].push(value);
	});

	return params;
}