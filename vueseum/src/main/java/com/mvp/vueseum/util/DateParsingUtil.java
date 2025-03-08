package com.mvp.vueseum.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateParsingUtil {
    // Common pattern elements
    private static final String CIRCA_PREFIX = "(?:circa|ca\\.?|c\\.?|about|approximately)\\s*";
    private static final String BC_SUFFIX = "(?:B\\.?C\\.?E?\\.?|BCE|BC)\\s*";
    private static final String AD_PREFIX = "(?:A\\.?D\\.?|CE)\\s*";
    private static final String DASH = "[\\-–—]"; // Includes regular hyphen, en-dash and em-dash

    // ================== BCE/BC DATE PATTERNS ==================

    // Special case: ca. X-Y B.C. (most specific first!)
    private static final Pattern CIRCA_BC_RANGE_PATTERN = Pattern.compile(
            CIRCA_PREFIX + "(\\d+)" + DASH + "\\d+\\s*" + BC_SUFFIX,
            Pattern.CASE_INSENSITIVE
    );

    // Special case: X-Y B.C.
    private static final Pattern BC_RANGE_PATTERN = Pattern.compile(
            "(\\d+)" + DASH + "\\d+\\s*" + BC_SUFFIX,
            Pattern.CASE_INSENSITIVE
    );

    // Various formats of X BCE/BC with different spacings and periods
    private static final Pattern BCE_BC_PATTERN = Pattern.compile(
            "(\\d+)\\s*" + BC_SUFFIX,
            Pattern.CASE_INSENSITIVE
    );

    // No-space BCE/BC formats like "500BC"
    private static final Pattern BCE_BC_NO_SPACE_PATTERN = Pattern.compile(
            "(\\d+)(BCE|BC)",
            Pattern.CASE_INSENSITIVE
    );

    // B.C. with extra spaces between periods
    private static final Pattern BC_SPACED_PATTERN = Pattern.compile(
            "(\\d+)\\s*B\\.\\s*C\\.\\s*",
            Pattern.CASE_INSENSITIVE
    );

    // ================== CE/AD DATE PATTERNS ==================

    // Circa CE dates
    private static final Pattern CIRCA_CE_PATTERN = Pattern.compile(
            CIRCA_PREFIX + "(1[0-9]{3}|20[0-2][0-9])",
            Pattern.CASE_INSENSITIVE
    );

    // A.D. X format
    private static final Pattern AD_PATTERN = Pattern.compile(
            AD_PREFIX + "(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // X CE format
    private static final Pattern CE_PATTERN = Pattern.compile(
            "(\\d+)\\s*" + AD_PREFIX,
            Pattern.CASE_INSENSITIVE
    );

    // Simple year pattern (1000-2029)
    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "\\b(1[0-9]{3}|20[0-2][0-9])\\b"
    );

    // ================== RANGE PATTERNS ==================

    // AD range pattern like "A.D. 500-600"
    private static final Pattern AD_RANGE_PATTERN = Pattern.compile(
            AD_PREFIX + "(\\d+)" + DASH + "(\\d+|present)",
            Pattern.CASE_INSENSITIVE
    );

    // Cross-era range like "30 B.C.–A.D. 364"
    private static final Pattern CROSS_ERA_RANGE_PATTERN = Pattern.compile(
            "(\\d+)\\s*" + BC_SUFFIX + DASH + AD_PREFIX + "\\d+",
            Pattern.CASE_INSENSITIVE
    );

    // General date range pattern
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile(
            "(\\d+)(?:\\s*(?:BCE|BC|CE|AD)?)?" + DASH + "(\\d+)(?:\\s*(?:BCE|BC|CE|AD)?)?",
            Pattern.CASE_INSENSITIVE
    );

    // Abbreviated ranges like "1910-15"
    private static final Pattern ABBREVIATED_RANGE = Pattern.compile(
            "(1[0-9]{3}|20[0-2][0-9])" + DASH + "(\\d{1,2})",
            Pattern.CASE_INSENSITIVE
    );

    // ================== CENTURY PATTERNS ==================

    private static final Pattern CENTURY_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)\\s+century",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EARLY_CENTURY = Pattern.compile(
            "early\\s+(\\d+)(?:st|nd|rd|th)\\s+century",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LATE_CENTURY = Pattern.compile(
            "late\\s+(\\d+)(?:st|nd|rd|th)\\s+century",
            Pattern.CASE_INSENSITIVE
    );

    // NEW: Century range pattern
    private static final Pattern CENTURY_RANGE_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)" + DASH + "\\d+(?:st|nd|rd|th)\\s+century",
            Pattern.CASE_INSENSITIVE
    );

    // ================== MILLENNIUM PATTERNS ==================

    private static final Pattern MILLENNIUM_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)?\\s+millennium\\s*(?:BCE|BC|CE|AD)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MILLENNIUM_RANGE_PATTERN = Pattern.compile(
            "(\\d+)(?:st|nd|rd|th)?" + DASH + "\\d+(?:st|nd|rd|th)?\\s+millennium\\s*(?:BCE|BC|CE|AD)?",
            Pattern.CASE_INSENSITIVE
    );

    // NEW: Early millennium range pattern
    private static final Pattern EARLY_MILLENNIUM_RANGE_PATTERN = Pattern.compile(
            "early\\s+(\\d+)(?:st|nd|rd|th)?" + DASH + "\\d+(?:st|nd|rd|th)?\\s+millennium",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts a year from various date string formats.
     * Negative years represent BCE/BC dates.
     * @param dateString The date string to parse
     * @return The extracted year (negative for BCE/BC)
     * @throws NumberFormatException if no valid year can be extracted
     */
    public static int extractYear(String dateString) {
        if (dateString == null) {
            throw new NumberFormatException("Date string is null");
        }

        String normalized = dateString.trim().toLowerCase();
        log.debug("Attempting to parse date: {}", normalized);

        // ================ BCE/BC PATTERNS ================

        // Try circa BC range pattern first (most specific)
        Matcher circaBCRangeMatcher = CIRCA_BC_RANGE_PATTERN.matcher(normalized);
        if (circaBCRangeMatcher.find()) {
            int year = Integer.parseInt(circaBCRangeMatcher.group(1));
            log.debug("Matched CIRCA_BC_RANGE_PATTERN: {}", year);
            return -year;
        }

        // Try BC range pattern
        Matcher bcRangeMatcher = BC_RANGE_PATTERN.matcher(normalized);
        if (bcRangeMatcher.find()) {
            int year = Integer.parseInt(bcRangeMatcher.group(1));
            log.debug("Matched BC_RANGE_PATTERN: {}", year);
            return -year;
        }

        // Try standard BCE/BC pattern
        Matcher bceMatcher = BCE_BC_PATTERN.matcher(normalized);
        if (bceMatcher.find()) {
            int year = Integer.parseInt(bceMatcher.group(1));
            log.debug("Matched BCE_BC_PATTERN: {}", year);
            return -year;
        }

        // Try no-space BCE/BC patterns
        Matcher bceNoSpaceMatcher = BCE_BC_NO_SPACE_PATTERN.matcher(normalized);
        if (bceNoSpaceMatcher.find()) {
            int year = Integer.parseInt(bceNoSpaceMatcher.group(1));
            log.debug("Matched BCE_BC_NO_SPACE_PATTERN: {}", year);
            return -year;
        }

        // Try B.C. with spaces between periods
        Matcher bcSpacedMatcher = BC_SPACED_PATTERN.matcher(normalized);
        if (bcSpacedMatcher.find()) {
            int year = Integer.parseInt(bcSpacedMatcher.group(1));
            log.debug("Matched BC_SPACED_PATTERN: {}", year);
            return -year;
        }

        // ================ CENTURY & MILLENNIUM ================

        // Handle century with qualifiers (check before other patterns)
        if (normalized.contains("century")) {
            int year = handleCenturyPatterns(normalized);
            log.debug("Matched century pattern: {}", year);
            return year;
        }

        // Handle millennium notation (check before other patterns)
        if (normalized.contains("millennium")) {
            int year = handleMillenniumPatterns(normalized);
            log.debug("Matched millennium pattern: {}", year);
            return year;
        }

        // ================ CROSS-ERA PATTERNS ================

        // Handle cross-era ranges like "30 B.C.–A.D. 364"
        Matcher crossEraMatcher = CROSS_ERA_RANGE_PATTERN.matcher(normalized);
        if (crossEraMatcher.find()) {
            int year = Integer.parseInt(crossEraMatcher.group(1));
            log.debug("Matched CROSS_ERA_RANGE_PATTERN: {}", year);
            return -year; // Return the BCE/BC year
        }

        // ================ CE/AD PATTERNS ================

        // Try circa CE pattern
        Matcher circaCEMatcher = CIRCA_CE_PATTERN.matcher(normalized);
        if (circaCEMatcher.find()) {
            int year = Integer.parseInt(circaCEMatcher.group(1));
            log.debug("Matched CIRCA_CE_PATTERN: {}", year);
            return year;
        }

        // Try AD format
        Matcher adMatcher = AD_PATTERN.matcher(normalized);
        if (adMatcher.find()) {
            int year = Integer.parseInt(adMatcher.group(1));
            log.debug("Matched AD_PATTERN: {}", year);
            return year;
        }

        // Try CE format
        Matcher ceMatcher = CE_PATTERN.matcher(normalized);
        if (ceMatcher.find()) {
            int year = Integer.parseInt(ceMatcher.group(1));
            log.debug("Matched CE_PATTERN: {}", year);
            return year;
        }

        // ================ RANGE PATTERNS ================

        // Handle period ranges (AD ranges)
        Matcher adRangeMatcher = AD_RANGE_PATTERN.matcher(normalized);
        if (adRangeMatcher.find()) {
            int year = Integer.parseInt(adRangeMatcher.group(1));
            log.debug("Matched AD_RANGE_PATTERN: {}", year);
            return year;
        }

        // Handle abbreviated ranges like "1910-15"
        Matcher abbreviatedRangeMatcher = ABBREVIATED_RANGE.matcher(normalized);
        if (abbreviatedRangeMatcher.find()) {
            int year = Integer.parseInt(abbreviatedRangeMatcher.group(1));
            log.debug("Matched ABBREVIATED_RANGE: {}", year);
            return year;
        }

        // ================ GENERAL PATTERNS ================

        // Check for regular year (should come after more specific patterns)
        Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
        if (yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            log.debug("Matched YEAR_PATTERN: {}", year);
            return year;
        }

        // ================ FALLBACK PATTERNS ================

        // Last attempt: general date ranges with no era markers
        Matcher rangeMatcher = YEAR_RANGE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            String startYearStr = rangeMatcher.group(1);
            boolean isBCE = normalized.contains("bce") || normalized.contains("bc");
            int startYear = Integer.parseInt(startYearStr);
            log.debug("Matched YEAR_RANGE_PATTERN: {}", startYear);
            return isBCE ? -startYear : startYear;
        }

        log.warn("Failed to extract year from: {}", dateString);
        throw new NumberFormatException("Could not extract year from: " + dateString);
    }

    /**
     * Helper method to handle different century patterns
     */
    private static int handleCenturyPatterns(String normalized) {
        // Try century range pattern first (most specific)
        Matcher centuryRangeMatcher = CENTURY_RANGE_PATTERN.matcher(normalized);
        boolean containsFormat = normalized.contains("bce") || normalized.contains("bc") ||
                normalized.contains("b.c");
        if (centuryRangeMatcher.find()) {
            int century = Integer.parseInt(centuryRangeMatcher.group(1));

            int year = (century - 1) * 100 + 50;
            return containsFormat ? -year : year;
        }

        Matcher earlyMatcher = EARLY_CENTURY.matcher(normalized);
        if (earlyMatcher.find()) {
            int century = Integer.parseInt(earlyMatcher.group(1));

            int year = (century - 1) * 100 + 25; // First quarter of century
            return containsFormat ? -year : year;
        }

        Matcher lateMatcher = LATE_CENTURY.matcher(normalized);
        if (lateMatcher.find()) {
            int century = Integer.parseInt(lateMatcher.group(1));

            int year = (century - 1) * 100 + 75; // Last quarter of century
            return containsFormat ? -year : year;
        }

        Matcher centuryMatcher = CENTURY_PATTERN.matcher(normalized);
        if (centuryMatcher.find()) {
            int century = Integer.parseInt(centuryMatcher.group(1));

            int year = (century - 1) * 100 + 50; // Mid-century
            return containsFormat ? -year : year;
        }

        throw new NumberFormatException("Could not parse century from: " + normalized);
    }

    /**
     * Helper method to handle millennium patterns
     */
    private static int handleMillenniumPatterns(String normalized) {
        // === EARLY MILLENNIUM PATTERNS ===

        // Early millennium range - most specific
        Matcher earlyMillenniumRangeMatcher = EARLY_MILLENNIUM_RANGE_PATTERN.matcher(normalized);
        boolean containsFormat = normalized.contains("bce") || normalized.contains("bc") ||
                normalized.contains("b.c");
        if (earlyMillenniumRangeMatcher.find()) {
            int millennium = Integer.parseInt(earlyMillenniumRangeMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 300); // Early in millennium (e.g., -4700 for 5th)
            } else {
                return (millennium - 1) * 1000 + 300;
            }
        }

        // Early millennium without range
        Pattern earlyMillenniumPattern = Pattern.compile(
                "early\\s+(\\d+)(?:st|nd|rd|th)?\\s+millennium",
                Pattern.CASE_INSENSITIVE
        );
        Matcher earlyMillenniumMatcher = earlyMillenniumPattern.matcher(normalized);
        if (earlyMillenniumMatcher.find()) {
            int millennium = Integer.parseInt(earlyMillenniumMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 300);
            } else {
                return (millennium - 1) * 1000 + 300;
            }
        }

        // === LATE MILLENNIUM PATTERNS ===

        // Late millennium
        Pattern lateMillenniumPattern = Pattern.compile(
                "late\\s+(\\d+)(?:st|nd|rd|th)?\\s+millennium",
                Pattern.CASE_INSENSITIVE
        );
        Matcher lateMillenniumMatcher = lateMillenniumPattern.matcher(normalized);
        if (lateMillenniumMatcher.find()) {
            int millennium = Integer.parseInt(lateMillenniumMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 700);
            } else {
                return (millennium - 1) * 1000 + 700;
            }
        }

        // === MID MILLENNIUM PATTERNS ===

        // Mid/middle millennium
        Pattern midMillenniumPattern = Pattern.compile(
                "(?:mid|middle of)\\s+(?:the\\s+)?(\\d+)(?:st|nd|rd|th)?\\s+millennium",
                Pattern.CASE_INSENSITIVE
        );
        Matcher midMillenniumMatcher = midMillenniumPattern.matcher(normalized);
        if (midMillenniumMatcher.find()) {
            int millennium = Integer.parseInt(midMillenniumMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 500); // Mid-point of millennium
            } else {
                return (millennium - 1) * 1000 + 500;
            }
        }

        // === SPECIFIC PORTIONS OF A MILLENNIUM ===

        // First half of millennium
        Pattern firstHalfPattern = Pattern.compile(
                "(?:first half|beginning) of(?: the)? (\\d+)(?:st|nd|rd|th)?\\s+millennium",
                Pattern.CASE_INSENSITIVE
        );
        Matcher firstHalfMatcher = firstHalfPattern.matcher(normalized);
        if (firstHalfMatcher.find()) {
            int millennium = Integer.parseInt(firstHalfMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 250);
            } else {
                return (millennium - 1) * 1000 + 250;
            }
        }

        // Second half of millennium
        Pattern secondHalfPattern = Pattern.compile(
                "(?:second half|end) of(?: the)? (\\d+)(?:st|nd|rd|th)?\\s+millennium",
                Pattern.CASE_INSENSITIVE
        );
        Matcher secondHalfMatcher = secondHalfPattern.matcher(normalized);
        if (secondHalfMatcher.find()) {
            int millennium = Integer.parseInt(secondHalfMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 750);
            } else {
                return (millennium - 1) * 1000 + 750;
            }
        }

        // === MILLENNIUM RANGE PATTERNS ===

        // Standard millennium range
        Matcher millenniumRangeMatcher = MILLENNIUM_RANGE_PATTERN.matcher(normalized);
        if (millenniumRangeMatcher.find()) {
            int millennium = Integer.parseInt(millenniumRangeMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 500); // Mid-point of first mentioned millennium
            } else {
                return (millennium - 1) * 1000 + 500;
            }
        }

        // === REGULAR MILLENNIUM PATTERN ===

        // Standard millennium (no qualifiers)
        Matcher millenniumMatcher = MILLENNIUM_PATTERN.matcher(normalized);
        if (millenniumMatcher.find()) {
            int millennium = Integer.parseInt(millenniumMatcher.group(1));

            if (containsFormat) {
                return -(millennium * 1000 - 500); // Mid-point
            } else {
                return (millennium - 1) * 1000 + 500;
            }
        }

        throw new NumberFormatException("Could not parse millennium from: " + normalized);
    }

    /**
     * Maps a specific year to a standardized period range.
     * Handles both CE and BCE dates.
     */
    public static String mapYearToPeriod(int year) {
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

    /**
     * Compares two years chronologically, with special handling for BCE years.
     *
     * @param year1 The first year (negative for BCE)
     * @param year2 The second year (negative for BCE)
     * @param ascending True for ascending (oldest first), false for descending (newest first)
     * @return negative integer if year1 should come before year2,
     *         positive integer if year1 should come after year2,
     *         zero if they are chronologically equal
     */
    public static int compareYearsChronologically(int year1, int year2, boolean ascending) {
        int result = Integer.compare(year1, year2);

        // For descending order, invert the result
        return ascending ? result : -result;
    }
}