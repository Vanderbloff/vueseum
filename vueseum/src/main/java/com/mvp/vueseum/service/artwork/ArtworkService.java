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
import com.mvp.vueseum.service.cultural.CulturalMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final Cache<String, List<String>> filterValueCache;

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

    public Map<String, List<String>> getFilterOptions(ArtworkSearchCriteria criteria) {
        Map<String, List<String>> options = new HashMap<>();
        try {
            // Always load all base options with a single database query
            options.put("objectType", artworkRepository.findDistinctClassifications());
            options.put("geographicLocations", artworkRepository.findDistinctGeographicLocations());
            options.put("mediums", artworkRepository.findDistinctMediums());
            options.put("regions", artworkRepository.findDistinctRegions());
            options.put("cultures", artworkRepository.findDistinctCultures());

            addSimplifiedCounts(options);
            log.info("Filter options structure: {}", options);
            return options;
        } catch (Exception e) {
            log.error("Error fetching filter options: {}", e.getMessage());
            return Map.of(
                    "objectType", new ArrayList<>(),
                    "geographicLocations", new ArrayList<>(),
                    "mediums", new ArrayList<>(),
                    "regions", new ArrayList<>(),
                    "cultures", new ArrayList<>()
            );
        }
    }

    private void validateClassification(String classification) {
        if (artworkRepository.findDistinctClassifications()
                .stream()
                .noneMatch(c -> c.startsWith(classification))) {
            throw new IllegalArgumentException("Invalid classification: " + classification);
        }
    }

    private void validateGeographicLocation(String location) {
        if (!CulturalMapping.getCulturalRegions().contains(location)) {
            throw new IllegalArgumentException("Invalid geographic location: " + location);
        }
    }

    private String getTopLevelCategory(String classification) {
        return classification.split("/")[0];
    }

    private void addSimplifiedCounts(Map<String, List<String>> options) {
        for (Map.Entry<String, List<String>> entry : options.entrySet()) {
            String filterType = entry.getKey();
            List<String> values = entry.getValue();
            List<String> valuesWithCounts = new ArrayList<>();

            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    long count = switch (filterType) {
                        case "objectType" -> artworkRepository.countByClassification(value);
                        case "mediums" -> artworkRepository.countByMedium(value);
                        case "geographicLocations" -> artworkRepository.countByGeographicLocation(value);
                        case "regions" -> artworkRepository.countByRegion(value);
                        case "cultures" -> artworkRepository.countByCulture(value);
                        default -> 0;
                    };
                    valuesWithCounts.add(String.format("%s (%d)", value, count));
                }
            }
            entry.setValue(valuesWithCounts);
        }
    }


    @Transactional(readOnly = true)
    public Page<ArtworkDetails> searchArtworks(ArtworkSearchCriteria criteria, Pageable pageable) {
        if (criteria.getSortField().equals("relevance")) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
        Page<Artwork> results = artworkRepository.findAll(spec, pageable);

        return new PageImpl<>(
                results.getContent().stream()
                        .map(this::convertToArtworkDetails)
                        .collect(Collectors.toList()),
                pageable,
                results.getTotalElements()
        );
    }

    /**
     * Finds potential artwork candidates for a tour based on basic criteria.
     * This method serves as the first step in tour generation, providing a filtered
     * pool of artworks that meet basic requirements before scoring and selection.
     *
     * @param preferences User's tour preferences including required artworks and filters
     * @return List of artwork candidates that meet basic criteria
     */
    public List<Artwork> findArtworkCandidates(TourPreferences preferences) {
        Specification<Artwork> spec = Objects.requireNonNull(
                ArtworkSpecifications.getThemeSpecificPreFilter(
                        preferences.getTheme(),
                        preferences.getMuseumId()
                )
        );

        if (!preferences.getPreferredArtists().isEmpty() ||
                !preferences.getPreferredPeriods().isEmpty() ||
                !preferences.getPreferredMediums().isEmpty() ||
                !preferences.getPreferredCultures().isEmpty()) {
            spec = spec.and(
                    ArtworkSpecifications.forTourPreferences(preferences));
        }

        List<Artwork> candidates = new ArrayList<>();
        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            candidates.addAll(
                    artworkRepository.findAllById(
                            preferences.getRequiredArtworkIds()
                    )
            );

            if (candidates.size() !=
                    preferences.getRequiredArtworkIds().size()) {
                throw new InvalidRequestException("Not all required artworks could be found");
            }
        }

        // Try with initial strict criteria
        int remainingNeeded = preferences.getMinStops() - candidates.size();
        if (remainingNeeded > 0) {
            Page<Artwork> initialCandidates = artworkRepository.findAll(
                    spec,
                    PageRequest.of(0, remainingNeeded, Sort.by("id"))
            );
            candidates.addAll(initialCandidates.getContent());
        }

        // If we don't have enough candidates, try relaxed constraints
        if (candidates.size() < preferences.getMinStops()) {
            // Get progressively relaxed specifications
            List<Specification<Artwork>> relaxationLevels =
                    ArtworkSpecifications.relaxConstraints(spec, preferences);

            // Try each relaxation level until we have enough candidates
            for (Specification<Artwork> relaxedSpec : relaxationLevels) {
                remainingNeeded = preferences.getMinStops() - candidates.size();
                Page<Artwork> additionalCandidates = artworkRepository.findAll(
                        relaxedSpec,
                        PageRequest.of(0, remainingNeeded, Sort.by("id"))
                );

                candidates.addAll(additionalCandidates.getContent());
                if (candidates.size() >= preferences.getMinStops()) {
                    break;
                }
            }

            // If we still don't have enough, throw exception
            if (candidates.size() < preferences.getMinStops()) {
                throw new InvalidRequestException(
                        "Could not find enough artworks matching the given preferences"
                );
            }
        }

        // Remove duplicates while preserving order
        return new ArrayList<>(new LinkedHashSet<>(candidates));
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
