package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.domain.TourGenerationProgress;
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
    private final Cache<String, Set<Long>> recentlyUsedArtworkCache;

    /**
     * Generates a tour, identifying the visitor via device fingerprint.
     */
    public Tour generateTour(TourGenerationRequest request, HttpServletRequest httpRequest) {
        String requestId = request.getRequestId();

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

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

        progressListener.updateProgress(requestId, 0.2, "selecting");

        List<Artwork> selectedArtworks = selectArtworks(
                request.getPreferences(),
                visitorId
        );

        progressListener.updateProgress(requestId, 0.6, "describing");
        String description = getOrGenerateDescription(request, selectedArtworks);
        progressListener.updateProgress(requestId, 0.65, "describing");
        return createTour(selectedArtworks, description, request.getPreferences(), requestId, visitorId);
    }

    /**
     * Gets a page of tours belonging to a specific device fingerprint
     */
    @Transactional(readOnly = true)
    public Page<Tour> getTourPageForDevice(String deviceFingerprint, Pageable pageable) {
        Pageable allContent = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return tourRepository.findByDeviceFingerprintAndDeletedFalse(deviceFingerprint, allContent);
    }

    /**
     * Selects artworks for the tour using a scoring-based approach with enhanced randomization.
     */
    private List<Artwork> selectArtworks(TourPreferences prefs, String visitorId) {
        // Get initial candidate pool
        List<Artwork> candidates = new ArrayList<>(artworkService.findArtworkCandidates(prefs));
        List<Artwork> selectedArtworks = new ArrayList<>();

        // Create random with consistent seed for reproducibility
        Random random = createRandomSeed(visitorId, prefs);

        // Get recently used artworks for diversity
        Set<Long> recentlyUsedArtworks = getRecentlyUsedArtworks(visitorId);

        // Handle specific required artworks first (highest priority)
        handleRequiredArtworks(candidates, selectedArtworks, prefs);

        // Add works by preferred artists (second priority)
        handlePreferredArtists(candidates, selectedArtworks, prefs);

        // Select other preference-based artworks (medium/culture/period)
        selectPreferenceBasedArtworks(
                candidates,
                selectedArtworks,
                prefs,
                recentlyUsedArtworks,
                random
        );

        // Fill remaining slots with general selections
        fillRemainingSlots(
                candidates,
                selectedArtworks,
                prefs,
                recentlyUsedArtworks,
                random
        );

        List<Artwork> optimizedArtworks = optimizeGalleryDistribution(selectedArtworks, prefs);
        updateRecentlyUsedArtworksCache(optimizedArtworks, recentlyUsedArtworks, visitorId);

        return optimizedArtworks;
    }

    /**
     * Creates a random number generator with a consistent seed.
     */
    private Random createRandomSeed(String visitorId, TourPreferences prefs) {
        long seed = System.currentTimeMillis() ^
                visitorId.hashCode() ^
                prefs.getTheme().hashCode();
        return new Random(seed);
    }

    /**
     * Retrieves recently used artworks for the given visitor.
     */
    private Set<Long> getRecentlyUsedArtworks(String visitorId) {
        return recentlyUsedArtworkCache.get(visitorId, _ -> new HashSet<>());
    }

    /**
     * Adds required artworks to the selection.
     */
    private void handleRequiredArtworks(
            List<Artwork> candidates,
            List<Artwork> selectedArtworks,
            TourPreferences prefs) {

        if (prefs.getRequiredArtworkIds().isEmpty()) {
            return;
        }

        log.info("Processing specific required artworks: {}", prefs.getRequiredArtworkIds());

        List<Artwork> requiredArtworks = candidates.stream()
                .filter(a -> prefs.getRequiredArtworkIds().contains(a.getId()))
                .toList();

        if (requiredArtworks.size() < prefs.getRequiredArtworkIds().size()) {
            Set<Long> foundIds = requiredArtworks.stream()
                    .map(Artwork::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(prefs.getRequiredArtworkIds());
            missingIds.removeAll(foundIds);
            log.warn("Could not find these required artwork IDs: {}", missingIds);
        }

        selectedArtworks.addAll(requiredArtworks);
        candidates.removeAll(selectedArtworks);

        // Add initial shuffle for diversity
        Collections.shuffle(candidates);
    }

    /**
     * Adds artworks from preferred artists to the selection with high priority.
     */
    private void handlePreferredArtists(
            List<Artwork> candidates,
            List<Artwork> selectedArtworks,
            TourPreferences prefs) {

        if (prefs.getPreferredArtists().isEmpty()) {
            return;
        }

        log.info("Processing preferred artists: {}", prefs.getPreferredArtists());
        for (String artistName : prefs.getPreferredArtists()) {
            if (selectedArtworks.size() >= prefs.getMaxStops()) {
                log.info("Tour is full, cannot add more artworks by preferred artists");
                break;
            }

            List<Artwork> artistWorks = candidates.stream()
                    .filter(a -> a.hasKnownArtist() &&
                            artistName.equals(a.getArtistName()))
                    .toList();

            if (artistWorks.isEmpty()) {
                log.warn("No artworks found for preferred artist: {}", artistName);
                continue;
            }

            Artwork selected = artistWorks.stream()
                    .filter(a -> a.getImageUrl() != null && !a.getImageUrl().isEmpty())
                    .findFirst()
                    .orElse(artistWorks.getFirst());

            log.info("Selected artwork '{}' by preferred artist '{}'",
                    selected.getTitle(), artistName);

            selectedArtworks.add(selected);
            candidates.remove(selected);
        }
    }

    /**
     * Selects artworks based on user preferences with priority.
     */
    private void selectPreferenceBasedArtworks(
            List<Artwork> candidates,
            List<Artwork> selectedArtworks,
            TourPreferences prefs,
            Set<Long> recentlyUsedArtworks,
            Random random) {

        if (selectedArtworks.size() >= prefs.getMaxStops()) {
            log.info("Tour already at maximum capacity, skipping preference-based selection");
            return;
        }

        // Extract preference-based candidates (medium/culture/period)
        List<Artwork> preferredCandidates = extractPreferredArtworks(candidates, prefs);
        log.info("Found {} artworks matching medium/culture/period preferences",
                preferredCandidates.size());

        // If we have preferred candidates, prioritize them
        while (selectedArtworks.size() < prefs.getMaxStops() && !preferredCandidates.isEmpty()) {
            Artwork selected = selectBestCandidate(
                    preferredCandidates,
                    selectedArtworks,
                    prefs,
                    recentlyUsedArtworks,
                    random
            );

            selectedArtworks.add(selected);
            preferredCandidates.remove(selected);
            candidates.remove(selected);
        }
    }

    /**
     * Extracts artworks that match user preferences for medium, culture, and period.
     * Artist preferences are handled separately with higher priority.
     */
    private List<Artwork> extractPreferredArtworks(List<Artwork> candidates, TourPreferences prefs) {
        List<Artwork> preferredCandidates = new ArrayList<>();

        // Filter by preferred mediums
        if (!prefs.getPreferredMediums().isEmpty()) {
            preferredCandidates.addAll(candidates.stream()
                    .filter(a -> a.getMedium() != null &&
                            prefs.getPreferredMediums().contains(a.getMedium()))
                    .toList());
        }

        // Filter by preferred cultures
        if (!prefs.getPreferredCultures().isEmpty()) {
            preferredCandidates.addAll(candidates.stream()
                    .filter(a -> a.getCulture() != null &&
                            prefs.getPreferredCultures().contains(a.getCulture()))
                    .toList());
        }

        // Remove duplicates while preserving order
        return preferredCandidates.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Selects the best candidate artwork based on scoring.
     */
    private Artwork selectBestCandidate(
            List<Artwork> candidates,
            List<Artwork> selectedArtworks,
            TourPreferences prefs,
            Set<Long> recentlyUsedArtworks,
            Random random) {

        // Pre-calculate scores for stable sorting
        List<Map.Entry<Artwork, Double>> scoredCandidates = candidates.stream()
                .map(artwork -> Map.entry(
                        artwork,
                        scoringService.scoreArtworkWithDiversity(
                                artwork,
                                prefs,
                                selectedArtworks,
                                recentlyUsedArtworks,
                                random
                        )
                ))
                .sorted(Map.Entry.<Artwork, Double>comparingByValue().reversed())
                .toList();

        // Select from top candidates with some randomness
        int topCount = Math.min(3, scoredCandidates.size());
        int randomIndex = random.nextInt(topCount);

        return scoredCandidates.get(randomIndex).getKey();
    }

    /**
     * Fills remaining tour slots after preference-based selection.
     */
    private void fillRemainingSlots(
            List<Artwork> candidates,
            List<Artwork> selectedArtworks,
            TourPreferences prefs,
            Set<Long> recentlyUsedArtworks,
            Random random) {

        int remainingSlots = prefs.getMaxStops() - selectedArtworks.size();

        if (remainingSlots <= 0) {
            log.info("No remaining slots to fill");
            return;
        }

        log.info("Filling {} remaining slots with general selections", remainingSlots);
        while (selectedArtworks.size() < prefs.getMaxStops() && !candidates.isEmpty()) {
            Artwork selected = selectBestCandidate(
                    candidates,
                    selectedArtworks,
                    prefs,
                    recentlyUsedArtworks,
                    random
            );

            selectedArtworks.add(selected);
            candidates.remove(selected);
        }

        log.info("Final selection contains {} artworks", selectedArtworks.size());
    }

    /**
     * Updates the cache of recently used artworks.
     */
    private void updateRecentlyUsedArtworksCache(
            List<Artwork> selectedArtworks,
            Set<Long> recentlyUsedArtworks,
            String visitorId) {

        // Update with newly selected artworks
        Set<Long> updatedRecentlyUsed = new HashSet<>(recentlyUsedArtworks);
        Set<Long> finalUpdatedRecentlyUsed = updatedRecentlyUsed;
        selectedArtworks.forEach(artwork -> finalUpdatedRecentlyUsed.add(artwork.getId()));

        // Limit cache size (keep most recent 30 artworks)
        if (updatedRecentlyUsed.size() > 30) {
            updatedRecentlyUsed = updatedRecentlyUsed.stream()
                    .sorted((id1, id2) -> Long.compare(id2, id1))
                    .limit(30)
                    .collect(Collectors.toSet());
        }

        // Update the cache
        recentlyUsedArtworkCache.put(visitorId, updatedRecentlyUsed);
    }

    /**
     * Optimizes artwork selection for gallery distribution.
     * Ensures artworks are reasonably distributed across different galleries.
     */
    private List<Artwork> optimizeGalleryDistribution(List<Artwork> candidates, TourPreferences prefs) {
        List<Artwork> optimizedSelection = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        Map<String, Integer> galleryCount = new HashMap<>();

        int maxPerGallery = Math.max(2, (int)Math.ceil(prefs.getMaxStops() * 0.6));
        log.info("Setting maximum of {} artworks per gallery", maxPerGallery);

        // Get valid candidates with gallery numbers
        List<Artwork> validCandidates = candidates.stream()
                .filter(a -> a.getGalleryNumber() != null && !a.getGalleryNumber().isEmpty())
                .collect(Collectors.toList());

        // If proximity is preferred, sort by gallery number
        if (prefs.isPreferCloseGalleries()) {
            log.info("Proximity preferred: sorting candidates by gallery number");
            validCandidates.sort(Comparator.comparing(a -> {
                try {
                    return Integer.parseInt(a.getGalleryNumber().replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    return Integer.MAX_VALUE;
                }
            }));
        } else {
            log.info("Narrative flow preferred: keeping thematic ordering");
        }

        // First pass: Select artworks while respecting gallery limits
        for (Artwork artwork : validCandidates) {
            if (selectedIds.contains(artwork.getId())) {
                continue;
            }

            String gallery = artwork.getGalleryNumber();
            int currentCount = galleryCount.getOrDefault(gallery, 0);

            if (currentCount < maxPerGallery) {
                optimizedSelection.add(artwork);
                selectedIds.add(artwork.getId());
                galleryCount.put(gallery, currentCount + 1);

                if (optimizedSelection.size() >= prefs.getMaxStops()) {
                    break;
                }
            }
        }

        // If we still don't have enough artworks, try again with the remaining candidates
        if (optimizedSelection.size() < prefs.getMaxStops()) {
            for (Artwork artwork : validCandidates) {
                if (selectedIds.contains(artwork.getId())) {
                    continue;
                }

                optimizedSelection.add(artwork);
                selectedIds.add(artwork.getId());

                if (optimizedSelection.size() >= prefs.getMaxStops()) {
                    break;
                }
            }
        }

        // Finally, add artworks without gallery numbers if still needed
        if (optimizedSelection.size() < prefs.getMaxStops()) {
            List<Artwork> noGalleryArtworks = candidates.stream()
                    .filter(a -> a.getGalleryNumber() == null || a.getGalleryNumber().isEmpty())
                    .filter(a -> !selectedIds.contains(a.getId()))
                    .toList();

            for (Artwork artwork : noGalleryArtworks) {
                optimizedSelection.add(artwork);
                selectedIds.add(artwork.getId()); // Track the ID

                if (optimizedSelection.size() >= prefs.getMaxStops()) {
                    break;
                }
            }
        }

        log.info("Gallery distribution: {}", galleryCount);
        return optimizedSelection;
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
        return String.format("%s-%s-%s-%s",
                request.getVisitorId(),
                request.getPreferences().getTheme(),
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
        Tour tour = new Tour();
        tour.setDeviceFingerprint(visitorId);
        tour.setTheme(prefs.getTheme());
        tour.setMuseum(artworks.getFirst().getMuseum());

        String title;
        if (description.startsWith("TITLE:")) {
            int newlineIndex = description.indexOf('\n');
            if (newlineIndex > 0) {
                title = description.substring(6, newlineIndex).trim();

                // Remove quotes if present (handles both single and double quotes)
                title = title.replaceAll("^[\"']|[\"']$", "");

                // Remove any remaining quotes
                title = title.replace("\"", "").replace("'", "");

                if (description.length() > newlineIndex + 1) {
                    description = description.substring(newlineIndex + 1).trim();
                    while (description.startsWith("\n")) {
                        description = description.substring(1);
                    }
                }
            } else {
                // Fallback if format is unexpected
                title = generateFallbackTitle(prefs);
            }
        } else {
            // Fallback if no title found
            title = generateFallbackTitle(prefs);
        }

        tour.setName(title);
        tour.setDescription(description);


        progressListener.updateStopInfo(requestId, 0, artworks.size());

        // Add stops in sequence with progress updates for each stop
        for (int i = 0; i < artworks.size(); i++) {
            progressListener.updateStopInfo(requestId, i, artworks.size());
            tour.addStop(artworks.get(i), i + 1);
        }

        tour.generateDescriptionsForAllStops(
                descriptionService,
                (currentIndex, totalStops) -> {
                    double progress = 0.65 + (0.25 * currentIndex / totalStops);
                    progressListener.updateProgress(requestId, progress, "describing");
                    progressListener.updateStopInfo(requestId, currentIndex, totalStops);
                }
        );

        progressListener.updateProgress(requestId, 0.9, "finalizing");
        Tour savedTour = tourRepository.save(tour);
        progressListener.updateProgress(requestId, 1.0, "complete");
        return savedTour;
    }

    private String generateFallbackTitle(TourPreferences prefs) {
        return String.format("%s: A Curated Selection - %s",
                prefs.getTheme().title,
                LocalDateTime.now().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")));
    }

    /**
     * Finds a tour by ID, but only if it belongs to the specified device
     */
    @Transactional(readOnly = true)
    public Optional<Tour> findTourByIdForDevice(Long id, String deviceFingerprint) {
        return tourRepository.findByIdAndDeviceFingerprintAndDeletedFalse(id, deviceFingerprint);
    }

    /**
     * Updates a tour's details, but only if it belongs to the specified device
     */
    @Transactional
    public Optional<Tour> updateTourDetailsForDevice(Long id, TourUpdateRequest request, String deviceFingerprint) {
        return tourRepository.findByIdAndDeviceFingerprintAndDeletedFalse(id, deviceFingerprint)
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

    /**
     * Deletes a tour, but only if it belongs to the specified device
     * Returns true if found and deleted, false if not found
     */
    @Transactional
    public boolean deleteTourForDevice(Long id, String deviceFingerprint) {
        return tourRepository.findByIdAndDeviceFingerprintAndDeletedFalse(id, deviceFingerprint)
                .map(tour -> {
                    tour.markAsDeleted();
                    tourRepository.save(tour);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Validates a tour, but only if it belongs to the specified device
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateTourForDevice(Long id, String deviceFingerprint) {
        Tour tour = tourRepository.findByIdAndDeviceFingerprintAndDeletedFalse(id, deviceFingerprint)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + id));

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
        result.put("tourId", id);
        result.put("validatedAt", tour.getLastValidated());
        result.put("unavailableStops", unavailableStops);
        return result;
    }

    /**
     * Gets the progress data for a tour generation request, including derived stage information.
     */
    public Optional<Map<String, Object>> getProgressWithStage(String requestId, String deviceFingerprint) {
        return progressListener.getProgressForDevice(requestId, deviceFingerprint)
                .map(this::mapProgressToResponse);
    }

    /**
     * Maps internal progress data to a frontend-friendly response.
     */
    private Map<String, Object> mapProgressToResponse(TourGenerationProgress progress) {
        Map<String, Object> response = new HashMap<>();

        // Convert progress to percentage
        double progressPercentage = progress.getProgress() * 100;
        response.put("progress", progressPercentage);
        response.put("stage", progress.getStage());

        // Add stop information if available - use "currentStop" to match frontend expectations
        if (progress.getCurrentStopIndex() != null && progress.getTotalStops() != null) {
            response.put("currentStop", progress.getCurrentStopIndex());
            response.put("totalStops", progress.getTotalStops());
        }

        if (progress.isHasError()) {
            response.put("error", progress.getErrorMessage());
        }

        return response;
    }

    /**
     * Gets the device fingerprint from an HTTP request.
     */
    public String getDeviceFingerprintFromRequest(HttpServletRequest request) {
        return deviceFingerprintService.getStoredFingerprint(request);
    }
}