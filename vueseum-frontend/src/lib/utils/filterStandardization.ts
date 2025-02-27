/**
 * Standardizes filter option text by removing qualifiers and normalizing format
 */
export function standardizeFilterOption(text: string): string {
	if (!text) return '';

	// Remove uncertainty qualifiers
	return text
		.replace(/\b(probably|possibly|attributed to|perhaps)\b/i, '')
		.replace(/\bpresent-day\b/i, '')
		.replace(/\s{2,}/g, ' ') // Remove extra spaces
		.trim();
}

/**
 * Standardizes common country names
 */
export function standardizeCountryName(name: string): string {
	if (!name) return '';

	// Common country name variations
	const countryMap: Record<string, string> = {
		'usa': 'United States',
		'united states of america': 'United States',
		'u.s.a.': 'United States',
		'u.s.': 'United States',
		'uk': 'United Kingdom',
		'great britain': 'United Kingdom',
		'england': 'United Kingdom',
		'britain': 'United Kingdom',
		'ussr': 'Russia',
		'soviet union': 'Russia',
		'holland': 'Netherlands',
		'republic of ireland': 'Ireland',
		'republic of korea': 'South Korea',
		'democratic republic of the congo': 'Congo',
		'people\'s republic of china': 'China',
		'czechoslovakia': 'Czech Republic'
	};

	const lowercaseName = name.toLowerCase();
	return countryMap[lowercaseName] || name;
}

/**
 * Extracts the count from a filter option string
 */
export function extractCount(option: string): number {
	const match = option.match(/\((\d+)\)$/);
	return match ? parseInt(match[1]) : 0;
}

/**
 * Extracts the clean value from a filter option string
 */
export function extractValue(option: string): string {
	return option.replace(/\s*\(\d+\)$/, '');
}

/**
 * Process filter options by standardizing them and combining duplicates
 */
export function standardizeFilterOptions(
	options: string[],
	type?: 'category' | 'origin'
): { value: string, label: string, count: number }[] {
	// Step 1: Extract values and counts
	const processedOptions = options.map(option => {
		const count = extractCount(option);
		const rawValue = extractValue(option);
		const standardizedValue = standardizeFilterOption(rawValue);

		return {
			originalValue: rawValue,
			standardizedValue,
			count
		};
	});

	// Step 2: Group by standardized value and sum counts
	const groupedOptions = new Map<string, number>();

	if (type === 'origin') {
		processedOptions.forEach(option => {
			option.standardizedValue = standardizeCountryName(option.standardizedValue);
		});
	}

	processedOptions.forEach(option => {
		if (!option.standardizedValue) return;

		const currentCount = groupedOptions.get(option.standardizedValue) || 0;
		groupedOptions.set(option.standardizedValue, currentCount + option.count);
	});

	// Step 3: Convert to final format and filter empty or zero count options
	return Array.from(groupedOptions.entries())
		.filter(([value, count]) => value && count > 0)
		.map(([value, count]) => ({
			value,
			label: value,
			count
		}))
		.sort((a, b) => b.count - a.count);
}