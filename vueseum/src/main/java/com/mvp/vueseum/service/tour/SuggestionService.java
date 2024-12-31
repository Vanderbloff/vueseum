package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.repository.ArtistRepository;
import com.mvp.vueseum.service.cultural.CulturalMapping;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SuggestionService {
    private final ArtistRepository artistRepository;
    private final FilterOptionsService filterOptionsService;
    private final Cache<String, List<Suggestion>> suggestionCache;

    public SuggestionService(ArtistRepository artistRepository,
                             FilterOptionsService filterOptionsService) {
        this.artistRepository = artistRepository;
        this.filterOptionsService = filterOptionsService;
        this.suggestionCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(1000)
                .build();
    }

    /**
     * Represents different types of suggestions we can provide.
     * This helps us organize our suggestion logic in one place.
     */
    public enum SuggestionType {
        ARTIST,
        MEDIUM,
        CULTURE,
        PERIOD
    }

    /**
     * A unified suggestion record that works for all types of suggestions.
     * The count field's meaning varies by type - for artists it's artwork count,
     * for mediums it might be how many pieces use that medium, etc.
     */
    public record Suggestion(
            String value,      // The actual value (e.g., artist name)
            String display,    // How it should be displayed in the UI
            long count,        // Relevant count for this suggestion
            SuggestionType type // What kind of suggestion this is
    ) {}

    public List<Suggestion> getSuggestions(String prefix,
                                            SuggestionType type,
                                            Long museumId,
                                            TourPreferences currentPreferences) {
        if (museumId == null) {
            throw new IllegalArgumentException("Museum ID cannot be null");
        }

        if (prefix == null || prefix.length() < 2) {
            return Collections.emptyList();
        }

        // Initialize default preferences if null
        TourPreferences prefs = currentPreferences != null ? currentPreferences : new TourPreferences();

        String cacheKey = generateCacheKey(prefix, type, museumId, prefs);
        return suggestionCache.get(cacheKey, _ -> computeSuggestions(prefix, type, museumId, prefs));
    }

    private List<Suggestion> computeSuggestions(String prefix,
                                                SuggestionType type,
                                                Long museumId,
                                                TourPreferences currentPreferences) {
        return switch (type) {
            case ARTIST -> getArtistSuggestions(prefix, museumId);
            case MEDIUM -> getMediumSuggestions(prefix, museumId, currentPreferences);
            case CULTURE -> getCultureSuggestions(prefix, museumId, currentPreferences);
            case PERIOD -> getPeriodSuggestions(prefix, museumId, currentPreferences);
        };
    }

    private List<Suggestion> getArtistSuggestions(String prefix, Long museumId) {
        return artistRepository.findSuggestedArtists(prefix, museumId)
                .stream()
                .map(artist -> new Suggestion(
                        artist.getArtistName(),
                        formatArtistDisplay(artist),
                        artist.getWorks().size(),
                        SuggestionType.ARTIST
                ))
                .toList();
    }

    private String formatArtistDisplay(Artist artist) {
        if (artist.getBirthDate() != null && artist.getDeathDate() != null) {
            return String.format("%s (%s-%s)",
                    artist.getArtistName(),
                    artist.hasValidLifespan() ? artist.getBirthDate() : "",
                    artist.getDeathDate());
        }
        return artist.getArtistName();
    }

    private List<Suggestion> getMediumSuggestions(String prefix,
                                                  Long museumId,
                                                  TourPreferences currentPreferences) {
        FilterOptionsService.FilterOptions options = filterOptionsService
                .getAvailableOptions(currentPreferences, museumId);

        return options.availableMediums().stream()
                .filter(medium -> matchesMedium(medium, prefix))
                .map(medium -> new Suggestion(
                        medium,
                        medium,
                        0,
                        SuggestionType.MEDIUM
                ))
                .toList();
    }

    private boolean matchesMedium(String medium, String prefix) {
        if (medium == null) return false;
        return medium.toLowerCase().contains(prefix.toLowerCase());
    }

    private List<Suggestion> getCultureSuggestions(String prefix,
                                                   Long museumId,
                                                   TourPreferences currentPreferences) {
        FilterOptionsService.FilterOptions options = filterOptionsService
                .getAvailableOptions(currentPreferences, museumId);

        return options.availableCultures().stream()
                .filter(culture -> matchesCulture(culture, prefix))
                .map(culture -> new Suggestion(
                        culture,
                        formatCultureDisplay(culture),
                        0,
                        SuggestionType.CULTURE
                ))
                .toList();
    }

    private boolean matchesCulture(String culture, String prefix) {
        if (culture == null) return false;

        String cultureLower = culture.toLowerCase();
        String prefixLower = prefix.toLowerCase();

        // Check direct containment
        if (cultureLower.contains(prefixLower)) return true;

        // Check related cultures from our knowledge base
        Set<String> relatedCountries = CulturalMapping.getCountriesForCulture(culture, true);
        return relatedCountries.stream()
                .map(String::toLowerCase)
                .anyMatch(country -> country.contains(prefixLower));
    }

    private List<Suggestion> getPeriodSuggestions(String prefix,
                                                  Long museumId,
                                                  TourPreferences currentPreferences) {
        FilterOptionsService.FilterOptions options = filterOptionsService
                .getAvailableOptions(currentPreferences, museumId);

        return options.availablePeriods().stream()
                .filter(period -> matchesPeriod(period, prefix))
                .map(period -> new Suggestion(
                        period,
                        formatPeriodDisplay(period),
                        0,
                        SuggestionType.PERIOD
                ))
                .toList();
    }

    private boolean matchesPeriod(String period, String prefix) {
        if (period == null) return false;

        String periodLower = period.toLowerCase();
        String prefixLower = prefix.toLowerCase();

        // Check for direct containment
        if (periodLower.contains(prefixLower)) return true;

        // Check for year matches
        Pattern yearPattern = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b");
        Matcher periodMatcher = yearPattern.matcher(period);

        while (periodMatcher.find()) {
            if (periodMatcher.group().contains(prefix)) return true;
        }

        return false;
    }

    private String formatCultureDisplay(String culture) {
        if (culture == null) return "";
        return CulturalMapping.getCultureContext(culture)
                .map(ctx -> String.format("%s (%s - %s)", culture, ctx.region(), ctx.subRegion()))
                .orElse(culture);
    }

    private String formatPeriodDisplay(String period) {
        // Add date ranges if available
        /*Pattern yearPattern = Pattern.compile("\\b(1[0-9]{3}|20[0-2][0-9])\\b");
        Matcher matcher = yearPattern.matcher(period);
        // Already contains year information
        matcher.find();*/
        return period;
    }

    private String generateCacheKey(String prefix,
                                    SuggestionType type,
                                    Long museumId,
                                    TourPreferences prefs) {
        return String.format("%s-%s-%d-%d",
                prefix.toLowerCase(),
                type,
                museumId,
                Objects.hash(prefs.getPreferredArtists(),
                        prefs.getPreferredMediums(),
                        prefs.getPreferredCultures(),
                        prefs.getPreferredPeriods())
        );
    }
}
