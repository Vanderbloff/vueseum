package com.mvp.vueseum.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParsingUtil {
    // Basic year patterns
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b");
    private static final Pattern BCE_PATTERN = Pattern.compile("(\\d+)\\s*(?:BCE|BC)\\b");
    private static final Pattern AD_PATTERN = Pattern.compile("A\\.D\\.\\s+(\\d+)");
    private static final Pattern CE_PATTERN = Pattern.compile("(\\d+)\\s*CE\\b");
    private static final Pattern BC_EXPLICIT_PATTERN = Pattern.compile("(\\d+)\\s*B\\.C\\.");

    // Century patterns
    private static final Pattern CENTURY_PATTERN = Pattern.compile("(\\d+)(st|nd|rd|th)\\s+century");
    private static final Pattern EARLY_CENTURY = Pattern.compile("early\\s+(\\d+)(st|nd|rd|th)\\s+century");
    private static final Pattern LATE_CENTURY = Pattern.compile("late\\s+(\\d+)(st|nd|rd|th)\\s+century");

    // Circa patterns
    private static final Pattern CIRCA_PATTERN = Pattern.compile("(?:circa|ca\\.|c\\.)\\s*(\\d+)(?:\\s*(?:BCE|BC|CE|AD)?)?");

    // Date range pattern
    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("(\\d+)(?:\\s*(?:BCE|BC|CE|AD)?)?-(\\d+)(?:\\s*(?:BCE|BC|CE|AD)?)?");

    // Special pattern for period ranges
    private static final Pattern AD_RANGE_PATTERN =
            Pattern.compile("A\\.D\\.\\s+(\\d+)-(\\d+|present)");
    private static final Pattern BC_RANGE_PATTERN =
            Pattern.compile("(\\d+)-(\\d+)\\s*B\\.C\\.");

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

        // Try CE format
        Matcher ceMatcher = CE_PATTERN.matcher(normalized);
        if (ceMatcher.find()) {
            return Integer.parseInt(ceMatcher.group(1));
        }

        // Try BC format
        Matcher bcRangeMatcher = BC_RANGE_PATTERN.matcher(normalized);
        if (bcRangeMatcher.find()) {
            // For BC range, extract the first year and negate it
            return -Integer.parseInt(bcRangeMatcher.group(1));
        }

        // Try AD format
        Matcher adMatcher = AD_PATTERN.matcher(normalized);
        if (adMatcher.find()) {
            return Integer.parseInt(adMatcher.group(1));
        }

        // Try explicit B.C. format
        Matcher bcExplicitMatcher = BC_EXPLICIT_PATTERN.matcher(normalized);
        if (bcExplicitMatcher.find()) {
            return -Integer.parseInt(bcExplicitMatcher.group(1));
        }

        // Handle period ranges
        Matcher adRangeMatcher = AD_RANGE_PATTERN.matcher(normalized);
        if (adRangeMatcher.find()) {
            return Integer.parseInt(adRangeMatcher.group(1));
        }

        // Check for BCE/BC dates
        Matcher bceMatcher = BCE_PATTERN.matcher(normalized);
        if (bceMatcher.find()) {
            return -Integer.parseInt(bceMatcher.group(1));
        }

        // Check for regular year
        Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
        if (yearMatcher.find()) {
            return Integer.parseInt(yearMatcher.group(1));
        }

        // Check for circa dates
        Matcher circaMatcher = CIRCA_PATTERN.matcher(normalized);
        if (circaMatcher.find()) {
            String yearStr = circaMatcher.group(1);
            int year = Integer.parseInt(yearStr);
            if (normalized.contains("bce") || normalized.contains("bc")) {
                return -year;
            }
            return year;
        }

        // Handle century with qualifiers
        if (normalized.contains("century")) {
            return handleCenturyPatterns(normalized);
        }

        // Handle date ranges by taking the earlier date
        Matcher rangeMatcher = YEAR_RANGE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            String startYearStr = rangeMatcher.group(1);
            boolean isBCE = normalized.contains("bce") || normalized.contains("bc");
            int startYear = Integer.parseInt(startYearStr);
            return isBCE ? -startYear : startYear;
        }

        throw new NumberFormatException("Could not extract year from: " + dateString);
    }

    /**
     * Helper method to handle different century patterns
     */
    private static int handleCenturyPatterns(String normalized) {
        Matcher earlyMatcher = EARLY_CENTURY.matcher(normalized);
        if (earlyMatcher.find()) {
            int century = Integer.parseInt(earlyMatcher.group(1));
            return (century - 1) * 100 + 25; // First quarter of century
        }

        Matcher lateMatcher = LATE_CENTURY.matcher(normalized);
        if (lateMatcher.find()) {
            int century = Integer.parseInt(lateMatcher.group(1));
            return (century - 1) * 100 + 75; // Last quarter of century
        }

        Matcher centuryMatcher = CENTURY_PATTERN.matcher(normalized);
        if (centuryMatcher.find()) {
            int century = Integer.parseInt(centuryMatcher.group(1));
            return (century - 1) * 100 + 50; // Mid-century
        }

        throw new NumberFormatException("Could not parse century from: " + normalized);
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
}