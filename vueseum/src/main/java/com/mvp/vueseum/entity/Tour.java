package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.baseEntity;
import com.mvp.vueseum.exception.AiProviderException;
import com.mvp.vueseum.service.DescriptionGenerationService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Slf4j
@Table(name = "tours",
        indexes = @Index(name = "idx_device_fingerprint", columnList = "device_fingerprint"))
public class Tour extends baseEntity {

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // The difficulty level of the tour (e.g., easy, moderate, challenging)
    @Enumerated(EnumType.STRING)
    private TourDifficulty difficulty = TourDifficulty.MODERATE;

    // The tour stops, ordered by sequence
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private Set<TourStop> stops = new LinkedHashSet<>();

    // Additional metadata stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // The museum this tour belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "museum_id", nullable = false)
    private Museum museum;

    public enum TourDifficulty {
        EASY,        // Minimal walking, suitable for all visitors
        MODERATE,    // Average walking distance, some stairs
        CHALLENGING  // Longer distances, multiple floors
    }

    @Column(name = "generation_prompt")
    private String generationPrompt; // Stores the prompt used to generate the description

    @Column(name = "tour_theme")
    @Enumerated(EnumType.STRING)
    private TourTheme theme;

    public enum TourTheme {
        CHRONOLOGICAL("Art through the ages", "Experience art's evolution across time"),
        ARTIST_FOCUSED("Featured artist spotlight", "Deep dive into an artist's work"),
        CULTURAL("Cultural perspectives", "Explore art across cultures");
        // Future Enhancement Consideration: HIGHLIGHTS
        // A highlights tour could showcase the museum's masterpieces and most significant works
        // Implementation would use isHighlight field from Met Museum API
        // Considerations for future implementation:
        // - Traffic flow management around popular pieces
        // - Time of day recommendations for viewing
        // - Integration with crowd density data
        // - Possible combination with other themes for "Highlight Artists" or "Cultural Highlights"

        public final String title;
        public final String description;

        TourTheme(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    // Helper method to add a stop to the tour
    public void addStop(Artwork artwork, Integer sequenceNumber) {
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative");
        }

        TourStop stop = new TourStop();
        stop.setTour(this);
        stop.setArtwork(artwork);
        stop.setSequenceNumber(sequenceNumber);
        stops.add(stop);

        // Convert to List for sorting
        List<TourStop> sortedStops = new ArrayList<>(stops);
        sortedStops.sort(Comparator.comparing(TourStop::getSequenceNumber));

        for (int i = 0; i < sortedStops.size(); i++) {
            sortedStops.get(i).setSequenceNumber(i);
        }

        // Clear and add back in sorted order
        stops.clear();
        stops.addAll(sortedStops);
    }

    public List<TourStop> getOrderedStops() {
        return stops.stream()
                .sorted(Comparator.comparing(TourStop::getSequenceNumber))
                .collect(Collectors.toList());
    }

    public void generateDescriptionsForAllStops(DescriptionGenerationService descriptionGenerationService) {
        for (TourStop stop : stops) {
            try {
                stop.setTourContextDescription(
                        descriptionGenerationService.generateStopDescription(stop)
                );
                if (stop.getStandardDescription() == null) {
                    Artwork artwork = stop.getArtwork();

                    if (artwork.getDescription() != null) {
                        stop.setStandardDescription(artwork.getDescription());
                    } else {
                        stop.setStandardDescription(descriptionGenerationService.generateArtworkDescription(artwork)
                        );
                    }
                }
            }
            catch (AiProviderException e) {
                log.error("Failed to generate descriptions for stop {}", stop.getSequenceNumber(), e);
                stop.setTourContextDescription("Description temporarily unavailable");
            }
        }
    }
}