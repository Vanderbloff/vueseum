package com.mvp.vueseum.service.cultural;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized cultural mapping system that provides methods for cultural relationship analysis
 * and geographical context lookup.
 */
public class CulturalMapping {
    public static final Map<String, CulturalRegion> CULTURAL_REGIONS = initializeCulturalRegions();
    public static final Map<String, String> COUNTRY_TO_REGION = buildCountryToRegionMap();

    private static Map<String, CulturalRegion> initializeCulturalRegions() {
        Map<String, CulturalRegion> regions = new HashMap<>();

        // Africa
        regions.put("Africa", new CulturalRegion("Africa", List.of(
                new CulturalRegion.SubRegion("Sub-Saharan Africa", Map.of(
                        "Zulu", Set.of("South Africa"),
                        "Maasai", Set.of("Kenya", "Tanzania"),
                        "Yoruba", Set.of("Nigeria"),
                        "Ashanti", Set.of("Ghana"),
                        "Hausa", Set.of("Nigeria", "Niger"),
                        "Igbo", Set.of("Nigeria")
                )),
                new CulturalRegion.SubRegion("North Africa", Map.of(
                        "Berber", Set.of("Morocco", "Algeria", "Tunisia", "Libya"),
                        "Tuareg", Set.of("Algeria", "Mali", "Niger", "Libya")
                ))
        )));

        regions.put("America", new CulturalRegion("America", List.of(
                new CulturalRegion.SubRegion("North America", Map.of(
                        "Inuit", Set.of("Canada", "Greenland"),
                        "Navajo", Set.of("United States")
                )),
                new CulturalRegion.SubRegion("South America", Map.of(
                        "Inca", Set.of("Peru", "Bolivia", "Ecuador"),
                        "Mapuche", Set.of("Chile", "Argentina")
                )),
                new CulturalRegion.SubRegion("Central America", Map.of(
                        "Maya", Set.of("Mexico", "Guatemala", "Belize", "Honduras"),
                        "Aztec", Set.of("Mexico")
                )),
                new CulturalRegion.SubRegion("Caribbean", Map.of(
                        "Taino", Set.of("Cuba", "Dominican Republic", "Puerto Rico")
                ))
        )));

        // East Asia
        regions.put("Asia", new CulturalRegion("Asia", List.of(
                new CulturalRegion.SubRegion("East Asia", Map.of(
                        "Han", Set.of("China"),
                        "Japanese", Set.of("Japan"),
                        "Korean", Set.of("South Korea", "North Korea"),
                        "Ainu", Set.of("Japan", "Russia")
                )),
                new CulturalRegion.SubRegion("Central Asia", Map.of(
                        "Mongol", Set.of("Mongolia", "China"),
                        "Turkic", Set.of("Turkey", "Uzbekistan", "Kazakhstan")
                )),
                new CulturalRegion.SubRegion("Middle East", Map.of(
                        "Assyrian", Set.of("Iraq", "Syria", "Turkey", "Iran"),
                        "Persian", Set.of("Iran")
                ))
        )));

        regions.put("Europe", new CulturalRegion("Europe", List.of(
                new CulturalRegion.SubRegion("Southern Europe", Map.of(
                        "Roman", Set.of("Italy"),
                        "Greek", Set.of("Greece")
                )),
                new CulturalRegion.SubRegion("Northern Europe", Map.of(
                        "Finnish", Set.of("Finland")
                )),
                new CulturalRegion.SubRegion("Western Europe", Map.of(
                        "Celtic", Set.of("Ireland", "Scotland", "Wales", "France"),
                        "Gaelic", Set.of("Ireland", "Scotland"),
                        "Basque", Set.of("Spain", "France")
                ))
        )));

        // Continue with other regions as needed
        return regions;
    }

    private static Map<String, String> buildCountryToRegionMap() {
        Map<String, String> countryMap = new HashMap<>();

        // Build reverse lookup from countries to regions
        for (CulturalRegion region : CULTURAL_REGIONS.values()) {
            for (CulturalRegion.SubRegion subRegion : region.getSubRegions()) {
                for (Set<String> countries : subRegion.culturesAndCountries().values()) {
                    for (String country : countries) {
                        countryMap.put(country, region.getName());
                    }
                }
            }
        }

        return countryMap;
    }

    /**
     * Determines how closely related two cultures are based on their regional proximity
     * and shared characteristics.
     */
    public static double calculateCulturalRelationship(String culture1, String culture2) {
        if (isInvalidCulture(culture1) || isInvalidCulture(culture2)) {
            return 0.0;
        }

        // Now we know both cultures are non-null and non-empty
        if (culture1.equals(culture2)) {
            return 1.0;
        }

        // Check if cultures share any countries - this is our strongest relationship after identity
        if (shareAnyCountries(culture1, culture2)) {
            return 0.8;  // Strongest relationship after direct equality
        }

        // If they don't share countries, check regional relationships
        Optional<String> sharedRegion = findSharedRegion(culture1, culture2);
        if (sharedRegion.isPresent()) {
            if (areInSameSubRegion(culture1, culture2, sharedRegion.get())) {
                return 0.7;  // Same sub-region but no shared countries
            }
            return 0.5;  // Same region, different sub-regions, no shared countries
        }

        return 0.1;  // Minimal relationship
    }

    private static boolean isInvalidCulture(String culture) {
        return culture == null || culture.trim().isEmpty() ||
                CULTURAL_REGIONS.values().stream()
                        .noneMatch(region -> region.getCultureToCountries().containsKey(culture));
    }

    private static Optional<String> findSharedRegion(String culture1, String culture2) {
        return CULTURAL_REGIONS.entrySet().stream()
                .filter(entry -> entry.getValue().getCultureToCountries().containsKey(culture1) &&
                        entry.getValue().getCultureToCountries().containsKey(culture2))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean areInSameSubRegion(String culture1, String culture2, String region) {
        return CULTURAL_REGIONS.get(region).getSubRegions().stream()
                .anyMatch(subRegion ->
                        subRegion.culturesAndCountries().containsKey(culture1) &&
                                subRegion.culturesAndCountries().containsKey(culture2));
    }

    private static boolean shareAnyCountries(String culture1, String culture2) {
        Set<String> countries1 = getCountriesForCulture(culture1);
        Set<String> countries2 = getCountriesForCulture(culture2);
        return !Collections.disjoint(countries1, countries2);
    }

    private static Set<String> getCountriesForCulture(String culture) {
        return CULTURAL_REGIONS.values().stream()
                .map(region -> region.getCultureToCountries().getOrDefault(culture, Set.of()))
                .reduce(new HashSet<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                });
    }

    /**
     * Gets the main region for a given country.
     */
    public static Optional<String> getRegionForCountry(String country) {
        return Optional.ofNullable(COUNTRY_TO_REGION.get(country));
    }

    /**
     * Gets all countries associated with a given culture.
     */
    public static Set<String> getCountriesForCulture(String culture, boolean includeRegionalCountries) {
        Set<String> directCountries = getCountriesForCulture(culture);
        if (!includeRegionalCountries) {
            return directCountries;
        }

        // Include countries from the same region
        Set<String> allCountries = new HashSet<>(directCountries);
        directCountries.stream()
                .map(COUNTRY_TO_REGION::get)
                .filter(Objects::nonNull)
                .forEach(region -> CULTURAL_REGIONS.get(region).getCultureToCountries().values()
                        .forEach(allCountries::addAll));

        return allCountries;
    }

    public static Optional<CultureContext> getCultureContext(String culture) {
        for (Map.Entry<String, CulturalRegion> entry : CULTURAL_REGIONS.entrySet()) {
            for (CulturalRegion.SubRegion subRegion : entry.getValue().getSubRegions()) {
                if (subRegion.culturesAndCountries().containsKey(culture)) {
                    return Optional.of(new CultureContext(entry.getKey(), subRegion.name()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all top-level cultural regions.
     * @return List of region names (e.g., "Africa", "Asia", "Europe")
     */
    public static List<String> getCulturalRegions() {
        return new ArrayList<>(CULTURAL_REGIONS.keySet());
    }

    /**
     * Gets all cultures associated with a specific region.
     *
     * @param region The name of the region (e.g., "Europe", "Asia")
     * @return List of culture names within that region
     * @throws IllegalArgumentException if the region doesn't exist
     */
    public static List<String> getCulturesForRegion(String region) {
        CulturalRegion culturalRegion = CULTURAL_REGIONS.get(region);
        if (culturalRegion == null) {
            throw new IllegalArgumentException("Invalid region: " + region);
        }

        return culturalRegion.getSubRegions().stream()
                .flatMap(subRegion -> subRegion.culturesAndCountries().keySet().stream())
                .collect(Collectors.toList());
    }

    public record CultureContext(String region, String subRegion) {}
}
