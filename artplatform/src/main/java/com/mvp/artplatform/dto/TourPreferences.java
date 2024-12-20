package com.mvp.artplatform.dto;

import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.exception.InvalidRequestException;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPreferences {
    private Tour.TourTheme theme;
    private Integer desiredDuration;

    // Accessibility preferences
    /*private boolean wheelchairAccessible;
    private boolean avoidStairs;
    private boolean requireElevator;*/

    // Content preferences with default empty collections
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

    // Tour constraints
    @Builder.Default
    private int maxStops = 10;

    @Builder.Default
    private int minStops = 3;

    /**
     * Validates that all preferences are consistent and within bounds
     */
    public void validate() {
        if (desiredDuration != null && desiredDuration < 15) {
            throw new InvalidRequestException("Tour duration must be at least 15 minutes");
        }

        if (maxStops < minStops) {
            throw new InvalidRequestException("Maximum stops cannot be less than minimum stops");
        }

        if (minStops < 1) {
            throw new InvalidRequestException("Minimum stops must be at least 1");
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
