package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.BaseEntity;
import com.mvp.vueseum.exception.AiProviderException;
import com.mvp.vueseum.service.DescriptionGenerationService;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Slf4j
@Table(name = "tours",
        indexes = @Index(name = "idx_device_fingerprint", columnList = "device_fingerprint"))
public class Tour extends BaseEntity {

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // The tour stops, ordered by sequence
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    @Getter(AccessLevel.NONE)
    private Set<TourStop> stops = new TreeSet<>(
            Comparator.comparing(TourStop::getSequenceNumber)
    );

    // Additional metadata stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // The museum this tour belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "museum_id", nullable = false)
    private Museum museum;

    @Column(name = "generation_prompt")
    private String generationPrompt;

    @Column(name = "tour_theme")
    @Enumerated(EnumType.STRING)
    private TourTheme theme;

    @Column
    private LocalDateTime lastValidated;

    public enum TourTheme {
        CHRONOLOGICAL("Art through the ages", "Experience art's evolution across time"),
        ARTIST_FOCUSED("Featured artist spotlight", "Deep dive into an artist's work"),
        CULTURAL("Cultural perspectives", "Explore art across cultures");

        public final String title;
        public final String description;

        TourTheme(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /*
     * TODO: Future Tour Stop Management
     * The current implementation using TreeSet maintains first-added stops when sequence numbers conflict.
     * Future functionality will need methods to:
     * 1. Replace stops - For when users want to modify specific stops
     * 2. Insert stops - For adding new stops between existing ones
     * 3. Re-sequence stops - For reordering tour stops
     *
     * Consider implementing:
     * - replaceStop(Artwork, sequenceNumber)
     * - insertStop(Artwork, desiredSequence)
     * - resequenceStops()
     */
    public void addStop(Artwork artwork, int sequenceNumber) {
        TourStop stop = new TourStop(this, artwork, sequenceNumber);
        stop.validateSequenceNumber(); // Validate before adding to set
        stops.add(stop);
    }

    public List<TourStop> getStops() {
        return new ArrayList<>(stops);
    }

    /**
     * Generates descriptions for all stops in the tour.
     *
     * @param descriptionGenerationService The service used to generate descriptions
     * @param progressCallback             Optional callback to report progress (current index, total stops)
     */
    public void generateDescriptionsForAllStops(
            DescriptionGenerationService descriptionGenerationService,
            BiConsumer<Integer, Integer> progressCallback
    ) {
        int index = 0;
        for (TourStop stop : stops) {
            try {
                stop.setTourContextDescription(
                        descriptionGenerationService.generateStopDescription(stop)
                );

                if (stop.getStandardDescription() == null || stop.getStandardDescription().isBlank()) {
                    Artwork artwork = stop.getArtwork();

                    if (artwork.getDescription() != null && !artwork.getDescription().isBlank()) {
                        stop.setStandardDescription(artwork.getDescription());
                    } else {
                        stop.setStandardDescription(descriptionGenerationService.generateArtworkDescription(artwork));
                    }
                }

                // Report progress after each stop is processed
                if (progressCallback != null) {
                    progressCallback.accept(index, stops.size());
                }
                index++;
            } catch (AiProviderException e) {
                log.error("Failed to generate descriptions for stop {}", stop.getSequenceNumber(), e);
                stop.setTourContextDescription("Description temporarily unavailable");

                // Still report progress even on error
                if (progressCallback != null) {
                    progressCallback.accept(index, stops.size());
                }
                index++;
            }
        }
    }

    /**
     * Original method for backward compatibility with existing code and tests
     */
    public void generateDescriptionsForAllStops(DescriptionGenerationService descriptionGenerationService) {
        generateDescriptionsForAllStops(descriptionGenerationService, null);
    }
}