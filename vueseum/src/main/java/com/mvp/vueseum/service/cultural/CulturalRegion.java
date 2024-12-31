package com.mvp.vueseum.service.cultural;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the hierarchical structure of cultural regions, helping to establish
 * relationships between different cultures and their geographical contexts.
 */
@Getter
public class CulturalRegion {
    private final String name;
    private final List<SubRegion> subRegions;
    private final Map<String, Set<String>> cultureToCountries;

    public CulturalRegion(String name, List<SubRegion> subRegions) {
        this.name = name;
        this.subRegions = subRegions;
        this.cultureToCountries = new HashMap<>();

        // Build the culture-to-countries mapping for quick lookup
        for (SubRegion subRegion : subRegions) {
            cultureToCountries.putAll(subRegion.culturesAndCountries());
        }
    }

    public record SubRegion(String name, Map<String, Set<String>> culturesAndCountries) {}
}

