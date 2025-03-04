package com.mvp.vueseum.domain;

import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.exception.InvalidRequestException;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPreferences {
    private Long museumId;
    private Tour.TourTheme theme;

    @Builder.Default
    private Set<Long> requiredArtworkIds = new HashSet<>();

    @Builder.Default
    private Set<String> preferredArtists = new HashSet<>();

    @Builder.Default
    private Set<String> preferredPeriods = new HashSet<>();

    @Builder.Default
    private Set<String> preferredMediums = new HashSet<>();

    @Builder.Default
    private Set<String> preferredCultures = new HashSet<>();

    @Builder.Default
    private boolean preferCloseGalleries = false;

    @Builder.Default
    private int maxStops = 10;

    @Builder.Default
    private int minStops = 3;

    /**
     * Validates that all preferences are consistent and within bounds
     */
    public void validate() {
        if (museumId == null) {
            throw new InvalidRequestException("Museum ID is required");
        }

        if (maxStops < minStops) {
            throw new InvalidRequestException("Maximum stops cannot be less than minimum stops");
        }

        if (minStops < 3) {
            throw new InvalidRequestException("Minimum stops must be at least 3");
        }

        if (!requiredArtworkIds.isEmpty() && requiredArtworkIds.size() > maxStops) {
            throw new InvalidRequestException(
                    "Number of required artworks cannot exceed maximum stops"
            );
        }

        if (theme == null) {
            throw new InvalidRequestException("Tour theme is required");
        }
    }
}
