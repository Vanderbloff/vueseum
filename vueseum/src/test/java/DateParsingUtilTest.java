import com.mvp.vueseum.util.DateParsingUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
}