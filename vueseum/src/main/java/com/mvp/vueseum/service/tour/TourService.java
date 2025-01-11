package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.exception.GenerationLimitExceededException;
import com.mvp.vueseum.exception.InvalidRequestException;
import com.mvp.vueseum.exception.TourLimitExceededException;
import com.mvp.vueseum.repository.TourRepository;
import com.mvp.vueseum.service.DescriptionGenerationService;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import com.mvp.vueseum.service.visitor.VisitorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TourService {
    private final DescriptionGenerationService descriptionService;
    private final ArtworkService artworkService;
    private final MuseumService museumService;
    private final VisitorTrackingService visitorTrackingService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final ScoringService scoringService;
    private final TourProgressListener progressListener;
    private final TourRepository tourRepository;

    // Single cache for tour descriptions
    private final Cache<String, String> descriptionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .maximumSize(1000)
            .build();

    public Tour generateTour(TourGenerationRequest request, HttpServletRequest httpRequest) {
        String requestId = UUID.randomUUID().toString();
        String visitorId = request.getVisitorId();
        progressListener.initializeProgress(requestId, visitorId);


        validateRequest(request);
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);
        handleVisitorTracking(request.getVisitorId(), deviceFingerprint);

        progressListener.updateProgress(requestId, 0.2, "Selecting artworks...");
        List<Artwork> selectedArtworks = selectArtworks(
                request.getPreferences(),
                visitorTrackingService.isReturningVisitor(request.getVisitorId())
        );

        progressListener.updateProgress(requestId, 0.6, "Filling in descriptions...");
        String description = getOrGenerateDescription(request, selectedArtworks);
        return createTour(selectedArtworks, description, request.getPreferences(), requestId, deviceFingerprint);
    }

    /**
     * Selects artworks for the tour using a scoring-based approach.
     * This method handles both required artworks and scored selection.
     */
    private List<Artwork> selectArtworks(TourPreferences prefs, boolean isReturningVisitor) {
        // Get initial candidate pool
        List<Artwork> candidates = new ArrayList<>(artworkService.findArtworkCandidates(prefs, isReturningVisitor));
        List<Artwork> selectedArtworks = new ArrayList<>();

        // First, handle required artworks
        candidates.stream()
                .filter(a -> prefs.getRequiredArtworkIds().contains(a.getId()))
                .forEach(selectedArtworks::add);

        // Remove selected artworks from candidates
        candidates.removeAll(selectedArtworks);

        // Then select remaining artworks based on scores
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

        return selectedArtworks;
    }

    /**
     * Validates the tour request and its preferences
     */
    private void validateRequest(TourGenerationRequest request) {
        if (request == null || request.getPreferences() == null) {
            throw new InvalidRequestException("Tour request and preferences are required");
        }
        if (!museumService.isValidMuseum(request.getPreferences().getMuseumId())) {
            throw new InvalidRequestException("Invalid museum ID");
        }
        request.getPreferences().validate();
    }

    /**
     * Handles visitor tracking and enforces generation limits
     */
    private void handleVisitorTracking(String visitorId, String fingerprint) {
        long totalTours = tourRepository.countByDeviceFingerprintAndDeletedFalse(fingerprint);
        if (totalTours >= 10 || !visitorTrackingService.recordTourGeneration(visitorId, fingerprint)) {
            String message = totalTours >= 10
                    ? "Maximum tour limit reached. Please delete an existing tour before creating a new one."
                    : "Daily tour generation limit reached. Please try again tomorrow.";
            throw totalTours >= 10
                    ? new TourLimitExceededException(message)
                    : new GenerationLimitExceededException(message);
        }
    }

    /**
     * Generates or retrieves a cached description for the tour
     */
    private String getOrGenerateDescription(TourGenerationRequest request, List<Artwork> artworks) {
        String cacheKey = generateCacheKey(request, artworks);
        return descriptionCache.get(cacheKey, _ ->
                descriptionService.generateTourDescription(artworks, request.getPreferences().getTheme())
        );
    }

     /**
     * Creates a cache key based on request parameters and selected artworks
     */
    private String generateCacheKey(TourGenerationRequest request, List<Artwork> artworks) {
        return String.format("%s-%s-%s",
                request.getVisitorId(),
                request.getPreferences().hashCode(),
                artworks.stream()
                        .map(Artwork::getId)
                        .sorted()
                        .map(Object::toString)
                        .collect(Collectors.joining("-"))
        );
    }

    /**
     * Creates a tour entity from the selected artworks and description
     */
    private Tour createTour(List<Artwork> artworks, String description, TourPreferences prefs, String requestId, String deviceFingerprint) {
        progressListener.updateProgress(requestId, 0.9, "Creating tour...");
        Tour tour = new Tour();
        tour.setDeviceFingerprint(deviceFingerprint);
        tour.setName(String.format("%s Tour - %s",
                prefs.getTheme(),
                LocalDateTime.now().toLocalDate()));
        tour.setDescription(description);
        tour.setMuseum(artworks.getFirst().getMuseum());
        tour.setTheme(prefs.getTheme());

        // Add stops in sequence
        for (int i = 0; i < artworks.size(); i++) {
            tour.addStop(artworks.get(i), i + 1);
        }

        tour.generateDescriptionsForAllStops(descriptionService);
        progressListener.updateProgress(requestId, 1.0, "Personalized tour completed!");
        return tour;
    }

    @Transactional(readOnly = true)
    public Optional<Tour> findTourById(Long id) {
        // Simple lookup with null-safety
        return tourRepository.findByIdAndDeletedFalse(id);
    }

    @Transactional
    public Optional<Tour> updateTourDetails(Long id, TourUpdateRequest request) {
        return tourRepository.findByIdAndDeletedFalse(id)
                .map(tour -> {
                    if (StringUtils.hasText(request.name())) {
                        tour.setName(request.name());
                    }
                    if (StringUtils.hasText(request.description())) {
                        tour.setDescription(request.description());
                    }
                    return tourRepository.save(tour);
                });
    }

    @Transactional
    public void cancelGeneration(String requestId) {
        // Mark generation as cancelled in progress tracking
        progressListener.updateProgress(requestId, 1.0, "Tour generation cancelled");
    }

    @Transactional
    public void deleteTour(Long id) {
        tourRepository.findByIdAndDeletedFalse(id)
                .ifPresent(tour -> {
                    tour.markAsDeleted(); // Uses baseEntity soft delete
                    tourRepository.save(tour);
                });
    }
}