// src/lib/utils/dateUtils.ts
export class DateUtils {
	private static readonly YEAR_PATTERN = /\b(1[0-9]{3}|20[0-2][0-9])\b/;
	private static readonly BCE_PATTERN = /(\d+)\s*(?:BCE|BC)\b/;
	private static readonly CENTURY_PATTERN = /(\d+)(st|nd|rd|th)\s+century/;
	private static readonly EARLY_CENTURY = /early\s+(\d+)(st|nd|rd|th)\s+century/;
	private static readonly LATE_CENTURY = /late\s+(\d+)(st|nd|rd|th)\s+century/;
	private static readonly CIRCA_PATTERN = /(?:circa|ca\.|c\.)\s*(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?/;
	private static readonly YEAR_RANGE_PATTERN = /(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?-(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?/;
	private static readonly AD_PATTERN = /A\.D\.\s+(\d+)/i;
	private static readonly CE_PATTERN = /(\d+)\s*CE\b/i;
	private static readonly BC_EXPLICIT_PATTERN = /(\d+)\s*B\.C\./i;
	private static readonly AD_RANGE_PATTERN = /A\.D\.\s+(\d+)-(\d+|present)/i;
	private static readonly BC_RANGE_PATTERN = /(\d+)-(\d+)\s*B\.C\./i;

	static extractYear(dateString: string): number {
		if (!dateString) {
			throw new Error("Date string is empty");
		}

		const normalized = dateString.trim().toLowerCase();

		const ceMatch = normalized.match(this.CE_PATTERN);
		if (ceMatch) {
			return parseInt(ceMatch[1]);
		}

		const adRangeMatch = normalized.match(this.AD_RANGE_PATTERN);
		if (adRangeMatch) {
			// Extract only the start year from A.D. range
			return parseInt(adRangeMatch[1]);
		}

		const bcRangeMatch = normalized.match(this.BC_RANGE_PATTERN);
		if (bcRangeMatch) {
			// For BC range, extract the first year and negate it
			return -parseInt(bcRangeMatch[1]);
		}

		const adMatch = normalized.match(this.AD_PATTERN);
		if (adMatch) {
			return parseInt(adMatch[1]);
		}

		const bcExplicitMatch = normalized.match(this.BC_EXPLICIT_PATTERN);
		if (bcExplicitMatch) {
			return -parseInt(bcExplicitMatch[1]);
		}

		// Check BCE/BC dates
		const bceMatch = normalized.match(this.BCE_PATTERN);
		if (bceMatch) {
			return -parseInt(bceMatch[1]);
		}

		// Check regular year
		const yearMatch = normalized.match(this.YEAR_PATTERN);
		if (yearMatch) {
			return parseInt(yearMatch[1]);
		}

		// Check circa dates
		const circaMatch = normalized.match(this.CIRCA_PATTERN);
		if (circaMatch) {
			const year = parseInt(circaMatch[1]);
			return normalized.includes('bc') ? -year : year;
		}

		// Handle century patterns
		if (normalized.includes('century')) {
			return this.handleCenturyPatterns(normalized);
		}

		// Handle date ranges by taking the earlier date
		const rangeMatch = normalized.match(this.YEAR_RANGE_PATTERN);
		if (rangeMatch) {
			const startYear = parseInt(rangeMatch[1]);
			return normalized.includes('bc') ? -startYear : startYear;
		}

		throw new Error(`Could not extract year from: ${dateString}`);
	}

	private static handleCenturyPatterns(normalized: string): number {
		const earlyMatch = normalized.match(this.EARLY_CENTURY);
		if (earlyMatch) {
			const century = parseInt(earlyMatch[1]);
			return (century - 1) * 100 + 25; // First quarter of century
		}

		const lateMatch = normalized.match(this.LATE_CENTURY);
		if (lateMatch) {
			const century = parseInt(lateMatch[1]);
			return (century - 1) * 100 + 75; // Last quarter of century
		}

		const centuryMatch = normalized.match(this.CENTURY_PATTERN);
		if (centuryMatch) {
			const century = parseInt(centuryMatch[1]);
			return (century - 1) * 100 + 50; // Mid-century
		}

		throw new Error(`Could not parse century from: ${normalized}`);
	}

	static mapYearToPeriod(year: number): string {
		if (year <= -1000) return "2000-1000 B.C.";
		if (year <= 1) return "1000 B.C.-A.D. 1";
		if (year <= 500) return "A.D. 1-500";
		if (year <= 1000) return "A.D. 500-1000";
		if (year <= 1400) return "A.D. 1000-1400";
		if (year <= 1600) return "A.D. 1400-1600";
		if (year <= 1800) return "A.D. 1600-1800";
		if (year <= 1900) return "A.D. 1800-1900";
		return "A.D. 1900-present";
	}

	static isYearInPeriod(year: number, periodStr: string): boolean {
		// Handle A.D. 1900-present format
		if (periodStr === "A.D. 1900-present") {
			return year >= 1900;
		}

		// Handle A.D. range format with explicit pattern
		const adRangeMatch = periodStr.match(/A\.D\.\s+(\d+)-(\d+|present)/i);
		if (adRangeMatch) {
			const startYear = parseInt(adRangeMatch[1]);
			const endStr = adRangeMatch[2];
			const endYear = endStr === 'present'
				? new Date().getFullYear()
				: parseInt(endStr);
			return year >= startYear && year <= endYear;
		}

		// Handle B.C. range pattern with explicit "B.C."
		const bcMatch = periodStr.match(/(\d+)\s*B\.C\.-/i);
		if (bcMatch) {
			const startYearBC = parseInt(bcMatch[1]);

			// Handle the combined B.C.-A.D. format
			const adPart = periodStr.match(/-A\.D\.\s+(\d+)/i);
			if (adPart) {
				const endYearAD = parseInt(adPart[1]);
				return year >= -startYearBC && year <= endYearAD;
			}
		}

		// Handle pure B.C. range
		const pureBCMatch = periodStr.match(/(\d+)-(\d+)\s*B\.C\./i);
		if (pureBCMatch) {
			const startYearBC = parseInt(pureBCMatch[1]);
			const endYearBC = parseInt(pureBCMatch[2]);
			// Note: BC years are negative in our system
			return year <= -endYearBC && year >= -startYearBC;
		}

		console.log(`Period: ${periodStr}, Year: ${year}, No pattern matched`);

		// Fallback for other formats
		const parts = periodStr.split('-')
			.map(p => p.trim())
			.map(p => {
				if (p === 'present') return new Date().getFullYear();
				if (p.includes('B.C.')) return -parseInt(p.replace(/\s*B\.C\./, ''));
				if (p.includes('A.D.')) return parseInt(p.replace(/A\.D\.\s+/, ''));
				return parseInt(p);
			});

		if (parts.length === 2) {
			return year >= parts[0] && year <= parts[1];
		}

		return false;
	}
}