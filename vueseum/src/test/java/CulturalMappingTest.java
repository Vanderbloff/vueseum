import com.mvp.vueseum.service.cultural.CulturalMapping;
import com.mvp.vueseum.service.cultural.CulturalRegion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CulturalMappingTest {

    @Test
    @DisplayName("Cultural regions should be properly initialized")
    void culturalRegionsAreInitialized() {
        List<String> regions = CulturalMapping.getCulturalRegions();
        assertThat(regions)
                .containsExactlyInAnyOrder("Africa", "America", "Asia", "Europe");

        CulturalRegion africa = CulturalMapping.CULTURAL_REGIONS.get("Africa");
        assertThat(Collections.singletonList(africa)).isNotNull();
        assertThat(africa.getSubRegions())
                .hasSize(2)
                .extracting("name")
                .containsExactlyInAnyOrder("Sub-Saharan Africa", "North Africa");
    }

    @Test
    @DisplayName("Cultural relationships should be correctly calculated")
    void calculateCulturalRelationships() {
        assertThat(CulturalMapping.calculateCulturalRelationship("Zulu", "Zulu"))
                .isEqualTo(1.0);

        assertThat(CulturalMapping.calculateCulturalRelationship("Hausa", "Igbo"))
                .isEqualTo(0.8);

        assertThat(CulturalMapping.calculateCulturalRelationship("Zulu", "Maasai"))
                .isEqualTo(0.7);

        assertThat(CulturalMapping.calculateCulturalRelationship("Berber", "Zulu"))
                .isEqualTo(0.5);

        assertThat(CulturalMapping.calculateCulturalRelationship("Zulu", "Maya"))
                .isEqualTo(0.1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"NonexistentCulture", "  ", "\t"})
    @DisplayName("Edge cases should be handled gracefully")
    void handleEdgeCases(String culture) {
        assertThat(CulturalMapping.calculateCulturalRelationship(culture, "Zulu"))
                .isEqualTo(0.0);

        assertThat(CulturalMapping.calculateCulturalRelationship("Zulu", culture))
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Country to region mapping should be consistent")
    void countryToRegionMappingIsConsistent() {
        assertThat(CulturalMapping.getRegionForCountry("Nigeria"))
                .contains("Africa");
        assertThat(CulturalMapping.getRegionForCountry("Japan"))
                .contains("Asia");

        Set<String> allCountries = CulturalMapping.COUNTRY_TO_REGION.keySet();
        assertThat(allCountries)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Culture context should be correctly retrieved")
    void getCultureContextWorks() {
        Optional<CulturalMapping.CultureContext> zuluContext =
                CulturalMapping.getCultureContext("Zulu");

        assertThat(zuluContext)
                .isPresent()
                .hasValueSatisfying(context -> {
                    assertThat(context.region()).isEqualTo("Africa");
                    assertThat(context.subRegion()).isEqualTo("Sub-Saharan Africa");
                });
    }

    @ParameterizedTest
    @CsvSource({
            "Zulu,South Africa",
            "Maya,Mexico",
            "Han,China"
    })
    @DisplayName("Countries for culture should be correctly retrieved")
    void getCountriesForCultureWorks(String culture, String expectedCountry) {
        Set<String> directCountries =
                CulturalMapping.getCountriesForCulture(culture, false);
        assertThat(directCountries).contains(expectedCountry);

        Set<String> regionalCountries =
                CulturalMapping.getCountriesForCulture(culture, true);
        assertThat(regionalCountries)
                .contains(expectedCountry)
                .hasSizeGreaterThan(directCountries.size());
    }

    @Test
    @DisplayName("Regional cultures should be correctly retrieved")
    void getCulturesForRegionWorks() {
        List<String> africanCultures = CulturalMapping.getCulturesForRegion("Africa");
        assertThat(africanCultures)
                .contains("Zulu", "Maasai", "Berber")
                .doesNotContain("Maya", "Han");

        assertThatThrownBy(() -> CulturalMapping.getCulturesForRegion("InvalidRegion"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid region");
    }
}