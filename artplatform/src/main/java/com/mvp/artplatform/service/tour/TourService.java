package com.mvp.artplatform.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.artplatform.dto.TourGenerationRequest;
import com.mvp.artplatform.dto.TourPreferences;
import com.mvp.artplatform.entity.Artwork;
import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.event.TourGenerationProgressEvent;
import com.mvp.artplatform.exception.GenerationLimitExceededException;
import com.mvp.artplatform.exception.InvalidRequestException;
import com.mvp.artplatform.service.DescriptionGenerationService;
import com.mvp.artplatform.service.artwork.ArtworkService;
import com.mvp.artplatform.service.visitor.DeviceFingerprintService;
import com.mvp.artplatform.service.visitor.VisitorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TourService {
    private final DescriptionGenerationService descriptionService;
    private final ArtworkService artworkService;
    private final VisitorTrackingService visitorTrackingService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final ScoringService scoringService;
    private final ApplicationEventPublisher eventPublisher;

    // Single cache for tour descriptions
    private final Cache<String, String> descriptionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .maximumSize(1000)
            .build();

    /**
     * Generates a tour based on user preferences and scoring of artwork candidates.
     * This method orchestrates the entire tour generation process.
     */
    public Tour generateTour(TourGenerationRequest request, HttpServletRequest httpRequest) {
        validateRequest(request);
        handleVisitorTracking(request.getVisitorId(), deviceFingerprintService.generateFingerprint(httpRequest));

        List<Artwork> selectedArtworks = selectArtworks(
                request.getPreferences(),
                visitorTrackingService.isReturningVisitor(request.getVisitorId())
        );

        String description = getOrGenerateDescription(request, selectedArtworks);
        return createTour(selectedArtworks, description, request.getPreferences());
    }

    /**
     * Selects artworks for the tour using a scoring-based approach.
     * This method handles both required artworks and scored selection.
     */
    private List<Artwork> selectArtworks(TourPreferences prefs, boolean isReturningVisitor) {
        // Get initial candidate pool
        publishProgress(0.1, TourGenerationProgressEvent.GenerationStage.CANDIDATE_SELECTION, "Finding artworks matching your preferences");
        List<Artwork> candidates = artworkService.findArtworkCandidates(prefs, isReturningVisitor);
        List<Artwork> selectedArtworks = new ArrayList<>();

        // First, handle required artworks
        candidates.stream()
                .filter(a -> prefs.getRequiredArtworkIds().contains(a.getId()))
                .forEach(selectedArtworks::add);

        // Remove selected artworks from candidates
        candidates.removeAll(selectedArtworks);

        // Then select remaining artworks based on scores
        publishProgress(0.3, TourGenerationProgressEvent.GenerationStage.ARTWORK_SCORING, "Evaluating artwork combinations");
        while (selectedArtworks.size() < prefs.getMaxStops() && !candidates.isEmpty()) {
            // Find the best next artwork based on scoring
            Artwork bestCandidate = candidates.stream()
                    .max((a1, a2) -> Double.compare(
                            scoringService.scoreArtwork(a1, prefs, selectedArtworks),
                            scoringService.scoreArtwork(a2, prefs, selectedArtworks)
                    ))
                    .orElseThrow(() -> new InvalidRequestException("Could not find suitable artwork"));

            selectedArtworks.add(bestCandidate);
            candidates.remove(bestCandidate);
        }

        publishProgress(0.8, TourGenerationProgressEvent.GenerationStage.TOUR_ASSEMBLY, "Creating your personalized tour");
        return selectedArtworks;
    }

    private void publishProgress(double progress, TourGenerationProgressEvent.GenerationStage stage, String message) {
        eventPublisher.publishEvent(new TourGenerationProgressEvent(
                this,
                progress,
                stage,
                message
        ));
    }

    /**
     * Validates the tour request and its preferences
     */
    private void validateRequest(TourGenerationRequest request) {
        if (request == null || request.getPreferences() == null) {
            throw new InvalidRequestException("Tour request and preferences are required");
        }
        request.getPreferences().validate();
    }

    /**
     * Handles visitor tracking and enforces generation limits
     */
    private void handleVisitorTracking(String visitorId, String fingerprint) {
        if (!visitorTrackingService.recordTourGeneration(visitorId, fingerprint)) {
            throw new GenerationLimitExceededException(
                    "Daily tour generation limit reached. Please try again tomorrow."
            );
        }
    }

    /**
     * Generates or retrieves a cached description for the tour
     */
    private String getOrGenerateDescription(TourGenerationRequest request, List<Artwork> artworks) {
        String cacheKey = generateCacheKey(request, artworks);
        return descriptionCache.get(cacheKey, k ->
                descriptionService.generateTourDescription(artworks, request.getPreferences().getTheme())
        );
    }

    // TODO: Consider caching descriptions of artworks to provide "standardized, curated" learning experience (i.e., descriptions shouldn't change much for an artwork per person)
    /**
     * Creates a cache key based on request parameters and selected artworks
     */
    private String generateCacheKey(TourGenerationRequest request, List<Artwork> artworks) {
        return String.format("%s-%s-%s-%s",
                request.getVisitorId(),
                request.getPreferences().hashCode(),
                artworks.stream()
                        .map(Artwork::getId)
                        .sorted()
                        .map(Object::toString)
                        .collect(Collectors.joining("-")),
                LocalDateTime.now().toLocalDate()
        );
    }

    /**
     * Creates a tour entity from the selected artworks and description
     */
    private Tour createTour(List<Artwork> artworks, String description, TourPreferences prefs) {
        Tour tour = new Tour();
        tour.setName(String.format("%s Tour - %s",
                prefs.getTheme(),
                LocalDateTime.now().toLocalDate()));
        tour.setDescription(description);
        tour.setMuseum(artworks.getFirst().getMuseum());
        tour.setTheme(prefs.getTheme());
        tour.setEstimatedDuration(prefs.getDesiredDuration());

        // Add stops in sequence
        for (int i = 0; i < artworks.size(); i++) {
            tour.addStop(artworks.get(i), i + 1);
        }

        tour.generateDescriptionsForAllStops(descriptionService);
        return tour;
    }
}