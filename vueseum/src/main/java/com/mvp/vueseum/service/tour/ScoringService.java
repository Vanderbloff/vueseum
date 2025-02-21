package com.mvp.vueseum.service.tour;

import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.service.cultural.CulturalMapping;
import com.mvp.vueseum.util.DateParsingUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ScoringService {

    /**
     * Main scoring method that combines different scoring components
     */
    public double scoreArtwork(Artwork artwork,
                               TourPreferences preferences,
                               List<Artwork> currentTourArtworks) {

        double themeScore = calculateThemeScore(artwork, preferences.getTheme()) * 0.3;
        double preferenceScore = calculatePreferenceScore(artwork, preferences) * 0.4;
        double flowScore = 0.0;

        if (!currentTourArtworks.isEmpty()) {
            flowScore += calculateFlowScore(
                    currentTourArtworks.getLast(),
                    artwork,
                    preferences.getTheme()
            ) * 0.3;
        }

        return themeScore + preferenceScore + flowScore;
    }

    /**
     * Calculates how well an artwork matches the selected theme.
     * This uses predefined art historical knowledge to make connections.
     */
    private double calculateThemeScore(Artwork artwork, Tour.TourTheme theme) {
        return switch (theme) {
            case CHRONOLOGICAL -> calculateChronologicalScore(artwork);
            case ARTIST_FOCUSED -> calculateArtistScore(artwork);
            case CULTURAL -> calculateCulturalScore(artwork);
        };
    }

    /**
     * Handles chronological scoring with more robust date parsing
     */
    private double calculateChronologicalScore(Artwork artwork) {
        String dateStr = artwork.getCreationDate();
        if (dateStr == null) return 0.1;

        try {
            DateParsingUtil.extractYear(dateStr);
            return 0.2;
        } catch (NumberFormatException e) {
            // If we can't parse year but have century/period info
            if (dateStr.matches(".*(century|BCE|CE|BC|AD).*")) {
                return 0.15;
            }
        }
        return 0.1;
    }

    private double calculateArtistScore(Artwork artwork) {
        return artwork.hasKnownArtist() ? 0.2 : 0.1;
    }

    private double calculateCulturalScore(Artwork artwork) {
        if (artwork.getCulture() == null) {
            return 0.1;
        }

        // Get all countries associated with this culture (including regional context)
        Set<String> relatedCountries = CulturalMapping.getCountriesForCulture(
                artwork.getCulture(),
                true
        );

        // Higher score for cultures with rich geographical context
        return 0.2 + (Math.min(relatedCountries.size(), 3) * 0.01);
    }

    /**
     * Calculates how well an artwork matches the user's explicit preferences.
     */
    private double calculatePreferenceScore(Artwork artwork, TourPreferences preferences) {
        double score = 0.0;

        if (artwork.hasKnownArtist() &&
                preferences.getPreferredArtists().contains(artwork.getArtistName())) {
            score += 0.15;
        }

        if (preferences.getPreferredPeriods().contains(artwork.getCreationDate())) {
            score += 0.2;
        }

        if (artwork.getMedium() != null &&
                preferences.getPreferredMediums().contains(artwork.getMedium())) {
            score += 0.3;
        }

        if (artwork.getCulture() != null &&
                preferences.getPreferredCultures().contains(artwork.getCulture())) {
            score += 0.4;
        }

        return score;
    }

    /**
     * Calculates how well an artwork flows from the previous artwork in the tour.
     * This helps create a coherent narrative throughout the tour.
     */
    private double calculateFlowScore(Artwork previous,
                                      Artwork current,
                                      Tour.TourTheme theme) {
        return switch (theme) {
            case CHRONOLOGICAL -> calculateChronologicalFlow(previous, current);
            case ARTIST_FOCUSED -> calculateArtistFlow(previous, current);
            case CULTURAL -> calculateCulturalFlow(previous, current);
        };
    }

    private double calculateChronologicalFlow(Artwork previous, Artwork current) {
        if (previous.getCreationDate() == null || current.getCreationDate() == null) {
            return 0.1;
        }

        try {
            int prevYear = DateParsingUtil.extractYear(previous.getCreationDate());
            int currentYear = DateParsingUtil.extractYear(current.getCreationDate());
            int yearDiff = Math.abs(currentYear - prevYear);

            if (yearDiff > 0 && yearDiff < 50) {
                return 0.2;  // Good progression within half a century
            } else if (yearDiff > 0 && yearDiff < 100) {
                return 0.15;  // Acceptable progression within a century
            }
        } catch (NumberFormatException e) {
            if (areSimilarPeriods(previous.getCreationDate(), current.getCreationDate())) {
                return 0.15;
            }
        }
        return 0.1;
    }

    private boolean areSimilarPeriods(String periodFromArtworkOne, String periodFromArtworkTwo) {
        if (periodFromArtworkOne == null || periodFromArtworkTwo == null) {
            return false;
        }

        // Normalize periods for comparison
        periodFromArtworkOne = periodFromArtworkOne.toLowerCase();
        periodFromArtworkTwo = periodFromArtworkTwo.toLowerCase();

        // If they're exactly the same period, return true
        if (periodFromArtworkOne.equals(periodFromArtworkTwo)) {
            return true;
        }

        // Check overlapping time periods based on art historical knowledge
        Map<String, Set<String>> relatedPeriods = Map.of(
                "renaissance", Set.of("early renaissance", "high renaissance", "late renaissance", "northern renaissance"),
                "baroque", Set.of("early baroque", "high baroque", "late baroque", "dutch golden age"),
                "modern", Set.of("post-impressionism", "art nouveau", "art deco", "modernism"),
                "medieval", Set.of("romanesque", "gothic", "early medieval", "late medieval")
        );

        // Check if periods belong to the same group
        String finalPeriodFromArtworkOne = periodFromArtworkOne;
        String finalPeriodFromArtworkTwo = periodFromArtworkTwo;
        return relatedPeriods.values().stream()
                .anyMatch(group -> group.contains(finalPeriodFromArtworkOne) && group.contains(finalPeriodFromArtworkTwo));
    }

    private double calculateArtistFlow(Artwork previous, Artwork current) {
        if (!previous.hasKnownArtist() || !current.hasKnownArtist()) {
            return 0.1;
        }

        if (previous.getArtistName().equals(current.getArtistName())) {
            return 0.2;
        }

        if (areRelatedArtists(previous.getArtist(), current.getArtist())) {
            return 0.15;
        }

        if (shareArtisticCharacteristic(previous.getArtist(), current.getArtist())) {
            return 0.15;
        }

        return 0.1;
    }

    private boolean shareCommonTags(Artist artist1, Artist artist2) {
        if (artist1 == null || artist2 == null ||
                !artist1.getAdditionalMetadata().containsKey("tags") ||
                !artist2.getAdditionalMetadata().containsKey("tags")) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<String> tags1 = (List<String>) artist1.getAdditionalMetadata().get("tags");
        @SuppressWarnings("unchecked")
        List<String> tags2 = (List<String>) artist2.getAdditionalMetadata().get("tags");

        if (tags1 == null || tags2 == null) {
            return false;
        }

        // Check for any overlapping tags
        return tags1.stream()
                .map(String::toLowerCase)
                .anyMatch(tag -> tags2.stream()
                        .map(String::toLowerCase)
                        .anyMatch(tag2 -> tag2.equals(tag)));
    }

    private boolean areRelatedArtists(Artist artist1, Artist artist2) {
        // Check if artists were contemporaries
        if (artist1.getBirthDate() != null && artist1.getDeathDate() != null
                && artist2.getBirthDate() != null && artist2.getDeathDate() != null) {

            if (artist1.hasReasonableLifespan() && artist2.hasReasonableLifespan()) {
                int artist1Start = Integer.parseInt(artist1.getBirthDate());
                int artist1End = Integer.parseInt(artist1.getDeathDate());
                int artist2Start = Integer.parseInt(artist2.getBirthDate());
                int artist2End = Integer.parseInt(artist2.getDeathDate());

                // Check if their lifetimes overlapped
                return (artist2Start <= artist1End && artist2End >= artist1Start);
            }
        }
        return false;
    }

    private boolean shareArtisticCharacteristic(Artist artist1, Artist artist2) {
        return Objects.equals(artist1.getNationality(), artist2.getNationality())
                || shareCommonTags(artist1, artist2);
    }

    private double calculateCulturalFlow(Artwork previous, Artwork current) {
        String prevCulture = previous.getCulture();
        String currCulture = current.getCulture();

        if (prevCulture == null || currCulture == null) {
            return 0.1;
        }

        return CulturalMapping.calculateCulturalRelationship(
                prevCulture,
                currCulture
        );
    }

}
