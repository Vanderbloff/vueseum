import com.mvp.vueseum.util.DateParsingUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateParsingUtilTest {

    @Test
    @DisplayName("Should extract year from simple year string")
    void extractsSimpleYear() {
        assertThat(DateParsingUtil.extractYear("1885"))
                .isEqualTo(1885);
    }

    @ParameterizedTest
    @CsvSource({
            "circa 1885, 1885",
            "ca. 1885, 1885",
            "c. 1885, 1885"
    })
    @DisplayName("Should extract year from circa dates")
    void extractsCircaYears(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "15th century, 1450",
            "19th century, 1850",
            "20th century, 1950"
    })
    @DisplayName("Should extract year from century notation")
    void extractsCenturyYears(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .isEqualTo(expectedYear);
    }

    @Test
    @DisplayName("Should throw exception for null input")
    void throwsExceptionForNullInput() {
        assertThatThrownBy(() -> DateParsingUtil.extractYear(null))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("Date string is null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid date",
            "not a year",
            "abc123"
    })
    @DisplayName("Should throw exception for invalid date formats")
    void throwsExceptionForInvalidFormats(String input) {
        assertThatThrownBy(() -> DateParsingUtil.extractYear(input))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("Could not extract year from");
    }

    @ParameterizedTest
    @CsvSource({
            "-1500, 2000-1000 B.C.",
            "-500, 1000 B.C.-A.D. 1",
            "250, A.D. 1-500",
            "750, A.D. 500-1000",
            "1200, A.D. 1000-1400",
            "1500, A.D. 1400-1600",
            "1700, A.D. 1600-1800",
            "1850, A.D. 1800-1900",
            "2000, A.D. 1900-present"
    })
    @DisplayName("Should map years to correct periods")
    void mapsYearsToPeriods(int year, String expectedPeriod) {
        assertThat(DateParsingUtil.mapYearToPeriod(year))
                .isEqualTo(expectedPeriod);
    }

    @ParameterizedTest
    @CsvSource({
            "early 15th century, 1425",
            "late 15th century, 1475",
            "mid-15th century, 1450",
            "15th century, 1450"
    })
    @DisplayName("Should handle century qualifiers with appropriate year values")
    void handlesCenturyQualifiers(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "500 BCE, -500",
            "500 BC, -500",
            "500 B.C., -500",
            "500 B.C, -500",
            "500BC, -500",
            "500BCE, -500",
            "500 B. C., -500"
    })
    @DisplayName("Should extract negative years from BCE/BC dates")
    void extractsBCEYears(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "1910-1920, 1910",
            "1910–1920, 1910",     // en-dash
            "1910—1920, 1910",     // em-dash
            "1910 to 1920, 1910",
            "1910-15, 1910",       // abbreviated range
            "1910-present, 1910",  // open-ended range
            "1876/1910, 1876"      // slash separator
    })
    @DisplayName("Should extract first year from CE date ranges")
    void extractsFirstYearFromCERanges(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "500-400 BCE, -500",
            "500–400 BC, -500",         // en-dash
            "500–400 B.C., -500",       // en-dash
            "2575–2520 B.C., -2575",    // en-dash
            "3rd-2nd century BCE, -250" // simplified interpretation
    })
    @DisplayName("Should extract first year from BCE/BC date ranges")
    void extractsFirstYearFromBCRanges(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "ca. 1500, 1500",
            "c. 1500, 1500",
            "ca. 1500 CE, 1500",
            "c. 1500 A.D., 1500",
            "ca. 500 BCE, -500",
            "ca. 500 B.C., -500",
            "circa 500 BCE, -500",
            "about 1920, 1920",
            "approximately 1900, 1900"
    })
    @DisplayName("Should extract years from various circa notations")
    void extractsYearsFromCircaNotations(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "ca. 1500-1600, 1500",
            "c. 1500-1520, 1500",
            "ca. 500-400 BCE, -500",
            "ca. 500–400 B.C., -500",       // en-dash
            "ca. 2575–2520 B.C., -2575",    // en-dash
            "circa 500-400 BCE, -500"
    })
    @DisplayName("Should extract first year from circa date ranges")
    void extractsFirstYearFromCircaRanges(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "30 B.C.–A.D. 364, -30",    // en-dash
            "30 BCE–10 CE, -30",        // en-dash
            "100 BC - 100 AD, -100",
            "1st century BCE - 1st century CE, -50"
    })
    @DisplayName("Should extract earliest year from cross-era date ranges")
    void extractsEarliestYearFromCrossEraRanges(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @ParameterizedTest
    @CsvSource({
            "3rd millennium BCE, -2500",  // mid-point of 3000-2000 BCE
            "5th–3rd millennium BCE, -4500", // first millennium mentioned
            "3rd millennium B.C., -2500",
            "1st millennium, 500",         // assumed CE
            "early 5th–3rd millennium BCE, -4700" // earlier portion of first millennium
    })
    @DisplayName("Should extract year from millennium notations")
    void extractsYearFromMillenniumNotations(String input, int expectedYear) {
        assertThat(DateParsingUtil.extractYear(input))
                .as("Processing date: %s", input)
                .isEqualTo(expectedYear);
    }

    @Test
    @DisplayName("Should correctly compare years chronologically")
    void comparesYearsChronologically() {
        // Ascending order (oldest first)
        assertThat(DateParsingUtil.compareYearsChronologically(-500, 500, true))
                .isLessThan(0); // BCE before CE
        assertThat(DateParsingUtil.compareYearsChronologically(-500, -200, true))
                .isLessThan(0); // Earlier BCE before later BCE
        assertThat(DateParsingUtil.compareYearsChronologically(500, 1500, true))
                .isLessThan(0); // Earlier CE before later CE

        // Descending order (newest first)
        assertThat(DateParsingUtil.compareYearsChronologically(-500, 500, false))
                .isGreaterThan(0); // CE before BCE
        assertThat(DateParsingUtil.compareYearsChronologically(-500, -200, false))
                .isGreaterThan(0);
        assertThat(DateParsingUtil.compareYearsChronologically(500, 1500, false))
                .isGreaterThan(0); // Later CE before earlier CE
    }

    @Test
    @DisplayName("Should sort dates in chronological order")
    void sortsDatesChronologically() {
        // Create a list of dates to sort
        List<String> dates = Arrays.asList(
                "1920",
                "ca. 2575–2520 B.C.",
                "15th century",
                "ca. 500 BCE",
                "30 B.C.–A.D. 364",
                "1800-1900"
        );

        // Expected years (for verification)
        List<Integer> expectedYears = Arrays.asList(1920, -2575, 1450, -500, -30, 1800);

        // Convert to year integers
        List<Integer> years = dates.stream()
                .map(date -> {
                    try {
                        return DateParsingUtil.extractYear(date);
                    } catch (Exception e) {
                        return 0; // Default for unparseable
                    }
                })
                .collect(Collectors.toList());

        // Verify extraction worked as expected
        assertThat(years).isEqualTo(expectedYears);

        // Sort ascending (oldest first)
        List<Integer> ascendingYears = new ArrayList<>(years);
        ascendingYears.sort((y1, y2) -> DateParsingUtil.compareYearsChronologically(y1, y2, true));
        assertThat(ascendingYears).containsExactly(-2575, -500, -30, 1450, 1800, 1920);

        // Sort descending (newest first)
        List<Integer> descendingYears = new ArrayList<>(years);
        descendingYears.sort((y1, y2) -> DateParsingUtil.compareYearsChronologically(y1, y2, false));
        assertThat(descendingYears).containsExactly(1920, 1800, 1450, -30, -500, -2575);
    }
}