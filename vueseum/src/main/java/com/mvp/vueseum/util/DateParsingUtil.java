package com.mvp.vueseum.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParsingUtil {
    /**
     * Extracts a year from various date string formats.
     * @param dateString The date string to parse
     * @return The extracted year
     * @throws NumberFormatException if no valid year can be extracted
     */
    public static int extractYear(String dateString) {
        if (dateString == null) {
            throw new NumberFormatException("Date string is null");
        }

        // Look for a clear year pattern first (e.g., "1885" or "2015")
        Pattern yearPattern = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b");
        Matcher matcher = yearPattern.matcher(dateString);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        // Handle century indicators (e.g., "15th century")
        if (dateString.toLowerCase().contains("century")) {
            Pattern centuryPattern = Pattern.compile("(\\d+)(st|nd|rd|th)\\s+century");
            matcher = centuryPattern.matcher(dateString.toLowerCase());
            if (matcher.find()) {
                int century = Integer.parseInt(matcher.group(1));
                return (century - 1) * 100 + 50; // Return mid-century as approximate date
            }
        }

        // Handle circa dates (e.g., "circa 1885" or "ca. 1885")
        if (dateString.toLowerCase().contains("circa") || dateString.toLowerCase().contains("ca.")) {
            Pattern circaPattern = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b");
            matcher = circaPattern.matcher(dateString);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        throw new NumberFormatException("Could not extract year from: " + dateString);
    }

    /**
     * Maps a specific year to a standardized period range.
     * @param year The year to map
     * @return The standardized period range
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
