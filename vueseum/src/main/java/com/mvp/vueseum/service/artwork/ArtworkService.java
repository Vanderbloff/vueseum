package com.mvp.vueseum.service.artwork;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.annotations.VisibleForTesting;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.exception.InvalidRequestException;
import com.mvp.vueseum.exception.PersistenceException;
import com.mvp.vueseum.exception.ResourceNotFoundException;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.service.artist.ArtistService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.specification.ArtworkSpecifications;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtistService artistService;
    private final MuseumService museumService;
    private final Cache<String, Artwork> artworkCache;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "artworks", key = "#details.externalId")
    public void saveFromDetails(ArtworkDetails details) {
        try {
            Artist artist = null;
            if (StringUtils.hasText(details.getArtistName())) {
                artist = artistService.findOrCreateArtist(details);
            }

            Museum museum = museumService.findOrCreateMuseum(details.getApiSource());
            Artwork artwork = artworkRepository.findByExternalIdAndMuseum(
                    details.getExternalId(),
                    museum
            ).orElseGet(() -> {
                Artwork newArtwork = new Artwork();
                newArtwork.setExternalId(details.getExternalId());
                return newArtwork;
            });

            artwork.setArtist(artist);
            artwork.setMuseum(museum);
            updateArtworkFromDetails(artwork, details);
            artwork.setProcessingStatus(Artwork.ProcessingStatus.COMPLETED);
            artwork.setLastSyncAttempt(LocalDateTime.now());

            Artwork savedArtwork = artworkRepository.save(artwork);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    artworkCache.put(savedArtwork.getExternalId(), savedArtwork);
                }
            });

        } catch (DataIntegrityViolationException e) {
            artworkCache.invalidate(details.getExternalId());
            throw new InvalidRequestException("Invalid artwork data: " + e.getMessage());
        }
        catch (PersistenceException e) {
            artworkCache.invalidate(details.getExternalId());
            throw new PersistenceException("Database error while saving artwork: " + e.getMessage());
        }
    }

    @Cacheable(value = "filterOptions", key = "'all'")
    public Map<String, List<String>> getFilterOptions(ArtworkSearchCriteria criteria) {
        Map<String, List<String>> options = new HashMap<>();
        final int CATEGORY_LIMIT = 200; // Higher limit for categories
        final int ORIGIN_LIMIT = 100;   // Standard limit for origins

        try {
            log.info("Cache miss for filter options - executing database query");

            // Get classification options with counts
            List<String> objectTypeOptions = convertToFormattedOptions(
                    artworkRepository.findClassificationsWithCountsLimited(CATEGORY_LIMIT)
            );
            options.put("objectType", objectTypeOptions);

            // Get medium options with counts
            List<String> materialsOptions = convertToFormattedOptions(
                    artworkRepository.findMediumsWithCountsLimited(CATEGORY_LIMIT)
            );
            options.put("materials", materialsOptions);

            // Get geographic location options with counts
            List<String> geographicOptions = convertToFormattedOptions(
                    artworkRepository.findGeographicLocationsWithCountsLimited(ORIGIN_LIMIT)
            );
            options.put("geographicLocations", geographicOptions);

            // Get region options with counts
            List<String> regionOptions = convertToFormattedOptions(
                    artworkRepository.findRegionsWithCountsLimited(ORIGIN_LIMIT)
            );
            options.put("regions", regionOptions);

            // Get culture options with counts
            List<String> cultureOptions = convertToFormattedOptions(
                    artworkRepository.findCulturesWithCountsLimited(ORIGIN_LIMIT)
            );
            options.put("cultures", cultureOptions);

            return options;
        } catch (Exception e) {
            log.error("Error fetching filter options: {}", e.getMessage(), e);
            return Map.of(
                    "objectType", new ArrayList<>(),
                    "geographicLocations", new ArrayList<>(),
                    "materials", new ArrayList<>(),
                    "regions", new ArrayList<>(),
                    "cultures", new ArrayList<>()
            );
        }
    }

    /**
     * Converts query results (name, count) into formatted strings: "Name (count)"
     */
    private List<String> convertToFormattedOptions(List<Object[]> queryResults) {
        return queryResults.stream()
                .map(result -> {
                    String name = (String) result[0];
                    Long count = (Long) result[1];
                    return String.format("%s (%d)", name, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ArtworkDetails> searchArtworks(ArtworkSearchCriteria criteria, Pageable pageable) {
        Page<Artwork> results;

        // Special case for date sorting
        if ("date".equals(criteria.getSortField())) {
            try {
                Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                boolean hasImage = criteria.getHasImage() != null ? criteria.getHasImage() : true;

                if (criteria.getSortDirection() == Sort.Direction.ASC) {
                    results = artworkRepository.findWithDateSortAsc(
                            hasImage,
                            criteria.getTitle(),
                            criteria.getOrigin(),
                            criteria.getCategory(),
                            unsortedPageable
                    );
                } else {
                    results = artworkRepository.findWithDateSortDesc(
                            hasImage,
                            criteria.getTitle(),
                            criteria.getOrigin(),
                            criteria.getCategory(),
                            unsortedPageable
                    );
                }

                // Fallback if the repository returns null
                if (results == null) {
                    log.warn("Date sorting repository method returned null, falling back to specification-based query");
                    Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
                    results = artworkRepository.findAll(spec, pageable);
                }
            } catch (Exception e) {
                log.error("Error executing date sort query: {}", e.getMessage());
                // Fallback to specification-based query
                Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
                results = artworkRepository.findAll(spec, pageable);
            }
        } else {
            // Regular path for other sort fields
            Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
            results = artworkRepository.findAll(spec, pageable);
        }

        return new PageImpl<>(
                results.getContent().stream()
                        .map(this::convertToArtworkDetails)
                        .collect(Collectors.toList()),
                pageable,
                results.getTotalElements()
        );
    }

    /**
     * Finds artwork candidates for a tour, ensuring enough results to meet minimum stops requirement.
     * Uses a progressive constraint relaxation approach if needed.
     */
    public List<Artwork> findArtworkCandidates(TourPreferences prefs) {
        // Start with the theme-specific filter (strongest constraints)
        Specification<Artwork> themeSpec = ArtworkSpecifications.getThemeSpecificPreFilter(prefs.getTheme(), prefs.getMuseumId());

        // Add any user preferences to create the ideal specification
        Specification<Artwork> idealSpec = themeSpec;
        if (!prefs.getPreferredArtists().isEmpty() || !prefs.getPreferredMediums().isEmpty() ||
                !prefs.getPreferredCultures().isEmpty() || !prefs.getPreferredPeriods().isEmpty()) {
            idealSpec = idealSpec.and(ArtworkSpecifications.forTourPreferences(prefs));
        }

        // Try to find candidates with the ideal constraints
        List<Artwork> candidates = artworkRepository.findAll(idealSpec);

        // If we have enough candidates, return them
        if (candidates.size() >= prefs.getMaxStops()) {
            log.info("Found {} candidates with ideal constraints (needed {})",
                    candidates.size(), prefs.getMaxStops());
            return candidates;
        }

        // If not enough candidates, try with theme-specific filter only
        if (candidates.size() < prefs.getMinStops()) {
            log.info("Not enough candidates with preferences ({} found, need {}). Relaxing to theme constraints.",
                    candidates.size(), prefs.getMinStops());

            List<Artwork> themeBasedCandidates = artworkRepository.findAll(themeSpec);

            Set<Long> existingIds = candidates.stream().map(Artwork::getId).collect(Collectors.toSet());
            themeBasedCandidates.stream()
                    .filter(a -> !existingIds.contains(a.getId()))
                    .forEach(candidates::add);

            // If we have enough now, return them
            if (candidates.size() >= prefs.getMinStops()) {
                log.info("Now have {} candidates after theme-based relaxation", candidates.size());
                return candidates;
            }
        }

        // If still not enough, use minimum constraints (museum ID and has image)
        log.info("Still not enough candidates ({}). Using minimum constraints.", candidates.size());

        Specification<Artwork> minimalSpec = (root, _, cb) -> {
            Join<Artwork, Museum> museumJoin = root.join("museum");
            return cb.and(
                    cb.equal(museumJoin.get("id"), prefs.getMuseumId()),
                    ArtworkSpecifications.createHasImagePredicate(root, cb)
            );
        };

        List<Artwork> minimalCandidates = artworkRepository.findAll(minimalSpec);

        // Add new unique candidates
        Set<Long> existingIds = candidates.stream().map(Artwork::getId).collect(Collectors.toSet());
        minimalCandidates.stream()
                .filter(a -> !existingIds.contains(a.getId()))
                .forEach(candidates::add);

        log.info("Final candidate count: {}", candidates.size());
        return candidates;
    }

    @SuppressWarnings("unchecked")
    private ArtworkDetails convertToArtworkDetails(Artwork artwork) {
        return ArtworkDetails.builder()
                .externalId(artwork.getExternalId())
                .title(artwork.getTitle())
                .creationYear(artwork.getCreationDate())

                // Artist information
                .artistName(artwork.getArtist().getArtistName())
                .artistNationality(artwork.getArtist().getNationality())
                .artistBirthYear(artwork.getArtist().getBirthDate())
                .artistDeathYear(artwork.getArtist().getDeathDate())
                .artistPrefix(artwork.getArtistPrefix())
                .artistRole(artwork.getArtistRole())

                // Basic artwork information
                .medium(artwork.getMedium())
                .artworkType(artwork.getClassification())
                .culture(artwork.getCulture())
                .country(artwork.getCountry())

                // Location and display
                .galleryNumber(artwork.getGalleryNumber())
                .department(artwork.getDepartment())

                // Description and image
                .description(artwork.getDescription())
                .primaryImageUrl(artwork.getImageUrl())
                .thumbnailImageUrl(artwork.getThumbnailImageUrl())

                // Museum source
                .apiSource(artwork.getMuseum() != null ? artwork.getMuseum().getName() : null)

                // Additional metadata
                .tags((List<String>) artwork.getAdditionalMetadata().getOrDefault("tags", new ArrayList<>()))
                .build();
    }

    @Transactional(readOnly = true)
    public ArtworkDetails fetchArtworkById(String id, Long museumId) {
        Museum museum = museumService.findMuseumById(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        Artwork artwork = artworkRepository.findByExternalIdAndMuseum(id, museum)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork not found"));

        return convertToArtworkDetails(artwork);
    }

    @Transactional(readOnly = true)
    public List<Artwork> findAllWithArtistsAndMuseums() {
        return artworkRepository.findAllWithArtistsAndMuseums();
    }

    private void updateArtworkFromDetails(Artwork artwork, ArtworkDetails details) {
        // Basic Information
        artwork.setTitle(details.getTitle());
        artwork.setClassification(details.getArtworkType());
        artwork.setMedium(details.getMedium());

        // Artist Attribution
        artwork.setArtistPrefix(details.getArtistPrefix());
        artwork.setArtistRole(details.getArtistRole());

        // Cultural and Geographic Context
        artwork.setCulture(details.getCulture());
        artwork.setCountry(details.getCountry());

        // Location and Display
        artwork.setGalleryNumber(details.getGalleryNumber());
        artwork.setDepartment(details.getDepartment());

        // Description and Image
        artwork.setDescription(details.getDescription());
        artwork.setImageUrl(details.getPrimaryImageUrl());
        artwork.setThumbnailImageUrl(details.getThumbnailImageUrl());
        artwork.setCreationDate(details.getCreationYear());

        // Additional Metadata
        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("tags", new ArrayList<>(details.getTags()));
        artwork.setAdditionalMetadata(additionalMetadata);
    }

    @Transactional
    public void recordProcessingError(String externalId, Long museumId, Exception error) {
        Museum museum = museumService.findMuseumById(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        // If it's a not found error, we don't need to do anything since
        // the artwork doesn't exist in our system
        if (error instanceof ResourceNotFoundException) {
            return;
        }

        String errorMessage = error != null ? error.getMessage() : "Unknown error";

        // Only try to record error if artwork exists
        artworkRepository.findByExternalIdAndMuseum(externalId, museum)
                .ifPresent(artwork -> {
                    artwork.setProcessingStatus(Artwork.ProcessingStatus.ERROR);
                    artwork.setLastSyncError(errorMessage);
                    artwork.setLastSyncAttempt(LocalDateTime.now());
                    artworkRepository.save(artwork);
                    log.debug("Recorded processing error for artwork {}: {}",
                            externalId, errorMessage);
                });
    }

    /**
     * Internal method to perform the actual artwork deletion.
     * Use other public methods to remove artworks based on specific criteria.
     */
    @Transactional
    private void doRemoveArtwork(Artwork artwork) {
        log.info("Removing artwork {} as it is no longer on display",
                artwork.getExternalId());
        artworkRepository.delete(artwork);
        artworkCache.invalidate(artwork.getExternalId());
    }

    /**
     * Bulk removal operation for artworks that are no longer on display.
     * Used during full sync operations to remove artworks that are no longer in the museum's display list.
     *
     * @param displayedIds Set of external IDs that are currently displayed
     * @param museumId ID of the museum to process
     * @throws ResourceNotFoundException if museum is not found
     */
    @Transactional
    public void removeNonDisplayedArtworks(Set<String> displayedIds, Long museumId) {
        museumService.findMuseumByIdForSync(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        findAllWithArtistsAndMuseums().stream()
                .filter(art -> art.getMuseum().getId().equals(museumId))
                .filter(art -> !displayedIds.contains(art.getExternalId()))
                .forEach(this::doRemoveArtwork);
    }

    @VisibleForTesting
    public void saveFromDetailsForTest(ArtworkDetails details) {
        try {
            Artist artist = null;
            if (StringUtils.hasText(details.getArtistName())) {
                artist = artistService.findOrCreateArtist(details);
            }

            Museum museum = museumService.findOrCreateMuseum(details.getApiSource());

            Artwork artwork = artworkRepository.findByExternalIdAndMuseum(
                    details.getExternalId(),
                    museum
            ).orElseGet(() -> {
                Artwork newArtwork = new Artwork();
                newArtwork.setExternalId(details.getExternalId());
                return newArtwork;
            });

            artwork.setArtist(artist);
            artwork.setMuseum(museum);
            updateArtworkFromDetails(artwork, details);
            artwork.setProcessingStatus(Artwork.ProcessingStatus.COMPLETED);
            artwork.setLastSyncAttempt(LocalDateTime.now());

            Artwork savedArtwork = artworkRepository.save(artwork);

            // Direct cache update instead of using transaction synchronization
            artworkCache.put(savedArtwork.getExternalId(), savedArtwork);

        } catch (InvalidRequestException e) {
            artworkCache.invalidate(details.getExternalId());
            throw e;
        } catch (Exception e) {
            artworkCache.invalidate(details.getExternalId());
            throw new PersistenceException("Database error while saving artwork: " + e.getMessage());
        }
    }
}
