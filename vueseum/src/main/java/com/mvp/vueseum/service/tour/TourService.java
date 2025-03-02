package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.exception.GenerationLimitExceededException;
import com.mvp.vueseum.exception.InvalidRequestException;
import com.mvp.vueseum.exception.ResourceNotFoundException;
import com.mvp.vueseum.exception.TourLimitExceededException;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.repository.TourRepository;
import com.mvp.vueseum.service.DescriptionGenerationService;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import com.mvp.vueseum.service.visitor.VisitorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TourService {
    private final DescriptionGenerationService descriptionService;
    private final ArtworkService artworkService;
    private final MuseumService museumService;
    private final VisitorTrackingService visitorTrackingService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final ScoringService scoringService;
    private final TourProgressListener progressListener;
    private final TourRepository tourRepository;
    private final ArtworkRepository artworkRepository;
    private final Cache<String, String> descriptionCache;

    public Tour generateTour(TourGenerationRequest request, HttpServletRequest httpRequest) {
        String requestId = UUID.randomUUID().toString();
        String clientProvidedId = request.getVisitorId();
        log.debug("Tour generation requested with client-provided visitorId: {}", clientProvidedId);

        String storedFingerprint = deviceFingerprintService.getStoredFingerprint(httpRequest);

        String visitorId;

        if (storedFingerprint != null) {
            log.debug("Found stored fingerprint from token cookie: {}", storedFingerprint);
            if (!storedFingerprint.equals(clientProvidedId)) {
                log.warn("Client-provided fingerprint doesn't match stored fingerprint");
                log.warn("Using stored fingerprint for consistency and security");
            }
            visitorId = storedFingerprint;
        } else {
            // Fall back to client-provided if we have no cookie (happens if cookies disabled)
            log.warn("No stored fingerprint found - using client-provided fingerprint");
            log.warn("This may cause inconsistent tour tracking if client changes fingerprints");
            visitorId = clientProvidedId;
        }

        progressListener.initializeProgress(requestId, visitorId);
        validateRequest(request);
        handleVisitorTracking(visitorId);

        progressListener.updateProgress(requestId, 0.2, "Selecting artworks...");
        List<Artwork> selectedArtworks = selectArtworks(
                request.getPreferences()
        );

        progressListener.updateProgress(requestId, 0.6, "Filling in descriptions...");
        String description = getOrGenerateDescription(request, selectedArtworks);
        return createTour(selectedArtworks, description, request.getPreferences(), requestId, visitorId);
    }

    @Transactional(readOnly = true)
    public Page<Tour> getTourPage(Pageable pageable) {
        Pageable allContent = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return tourRepository.findByDeletedFalse(allContent);
    }

    /**
     * Selects artworks for the tour using a scoring-based approach.
     * This method handles both required artworks and scored selection.
     */
    private List<Artwork> selectArtworks(TourPreferences prefs) {
        // Get initial candidate pool
        List<Artwork> candidates = new ArrayList<>(artworkService.findArtworkCandidates(prefs));
        List<Artwork> selectedArtworks = new ArrayList<>();

        Random random = new Random(System.currentTimeMillis());

        // First, handle required artworks
        candidates.stream()
                .filter(a -> prefs.getRequiredArtworkIds().contains(a.getId()))
                .forEach(selectedArtworks::add);

        // Remove selected artworks from candidates
        candidates.removeAll(selectedArtworks);

        // Shuffle candidates to introduce initial randomness
        Collections.shuffle(candidates, random);

        // Then select remaining artworks based on scores with random factor
        while (selectedArtworks.size() < prefs.getMaxStops() && !candidates.isEmpty()) {
            candidates.sort((a1, a2) -> Double.compare(
                    scoringService.scoreArtwork(a2, prefs, selectedArtworks),
                    scoringService.scoreArtwork(a1, prefs, selectedArtworks)
            ));

            // Select from top 30% candidates (or at least 3 candidates)
            int topCandidateCount = Math.max(3, (int)(candidates.size() * 0.3));
            int randomIndex = random.nextInt(Math.min(topCandidateCount, candidates.size()));

            Artwork selectedArtwork = candidates.get(randomIndex);
            selectedArtworks.add(selectedArtwork);
            candidates.remove(selectedArtwork);
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

        Long museumId = request.getPreferences().getMuseumId();
        if (!museumService.isValidMuseum(museumId)) {
            throw new InvalidRequestException("Invalid museum ID");
        }
        request.getPreferences().validate();
    }

    /**
     * Handles visitor tracking and enforces generation limits
     */
    private void handleVisitorTracking(String visitorId) {
        long totalTours = tourRepository.countByDeviceFingerprintAndDeletedFalse(visitorId);
        if (totalTours >= 10 || !visitorTrackingService.recordTourGeneration(visitorId)) {
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
    private Tour createTour(List<Artwork> artworks, String description, TourPreferences prefs, String requestId, String visitorId) {
        progressListener.updateProgress(requestId, 0.9, "Creating tour...");
        Tour tour = new Tour();
        tour.setDeviceFingerprint(visitorId);
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

        Tour savedTour = tourRepository.save(tour);
        progressListener.updateProgress(requestId, 1.0, "Personalized tour completed!");
        return savedTour;
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
    public void deleteTour(Long id) {
        tourRepository.findByIdAndDeletedFalse(id)
                .ifPresent(tour -> {
                    tour.markAsDeleted(); // Uses baseEntity soft delete
                    tourRepository.save(tour);
                });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateTour(Long tourId) {
        return tourRepository.findById(tourId)
                .map(tour -> {
                    List<Map<String, Object>> unavailableStops = tour.getStops().stream()
                            .filter(stop -> !artworkRepository.existsById(stop.getArtwork().getId()))
                            .map(stop -> {
                                Map<String, Object> stopInfo = new HashMap<>();
                                stopInfo.put("stopNumber", stop.getSequenceNumber());
                                stopInfo.put("artworkTitle", stop.getArtwork().getTitle());
                                stopInfo.put("galleryNumber", stop.getArtwork().getGalleryNumber());
                                return stopInfo;
                            })
                            .collect(Collectors.toList());

                    tour.setLastValidated(LocalDateTime.now());
                    tourRepository.save(tour);

                    Map<String, Object> result = new HashMap<>();
                    result.put("tourId", tourId);
                    result.put("validatedAt", tour.getLastValidated());
                    result.put("unavailableStops", unavailableStops);
                    return result;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));
    }
}