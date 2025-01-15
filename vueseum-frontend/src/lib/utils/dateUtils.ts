// src/lib/utils/dateUtils.ts
export class DateUtils {
	private static readonly YEAR_PATTERN = /\b(1[0-9]{3}|20[0-2][0-9])\b/;
	private static readonly BCE_PATTERN = /(\d+)\s*(?:BCE|BC)\b/;
	private static readonly CENTURY_PATTERN = /(\d+)(st|nd|rd|th)\s+century/;
	private static readonly EARLY_CENTURY = /early\s+(\d+)(st|nd|rd|th)\s+century/;
	private static readonly LATE_CENTURY = /late\s+(\d+)(st|nd|rd|th)\s+century/;
	private static readonly CIRCA_PATTERN = /(?:circa|ca\.|c\.)\s*(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?/;
	private static readonly YEAR_RANGE_PATTERN = /(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?-(\d+)(?:\s*(?:BCE|BC|CE|AD)?)?/;

	static extractYear(dateString: string): number {
		if (!dateString) {
			throw new Error("Date string is empty");
		}

		const normalized = dateString.trim().toLowerCase();

		// Check BCE/BC dates first
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
		if (periodStr.includes('B.C.')) {
			const numbers = periodStr.split('-')
				.map(p => parseInt(p.replace(/[^0-9]/g, '')));
			return year <= -numbers[1] && year >= -numbers[0];
		}

		const yearRange = periodStr
			.replace('A.D. ', '')
			.split('-')
			.map(p => p === 'present' ? new Date().getFullYear() : parseInt(p));

		return year >= yearRange[0] && year <= yearRange[1];
	}

	static formatDate(dateString: string | null): string {
		if (!dateString) return 'Date unknown';

		try {
			const year = this.extractYear(dateString);
			const period = this.mapYearToPeriod(year);
			return `${dateString} (${period})`;
		} catch (error) {
			return dateString;
		}
	}
}