package com.mvp.vueseum.service.artwork;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    private final Cache<String, Artwork> artworkCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .maximumSize(1000)
            .build();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "artworks", key = "#details.externalId")
    public void saveFromDetails(ArtworkDetails details) {
        // Find or create the artist
        try {
            Artist artist = null;
            if (StringUtils.hasText(details.getArtistName())) {
                artist = artistService.findOrCreateArtist(details);
            }

            // Find or create the museum
            Museum museum = museumService.findOrCreateMuseum(details.getApiSource());

            // Find or create the artwork
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

            Artwork saved = artworkRepository.save(artwork);
            artworkCache.put(artwork.getExternalId(), artwork);

        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("Invalid artwork data: " +
                    e.getMessage());
        }
        catch (PersistenceException e) {
            throw new PersistenceException("Database error while saving artwork: " +
                    e.getMessage());
        }
    }

    private final Cache<String, List<String>> filterOptionsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    public Map<String, List<String>> getFilterOptions(ArtworkSearchCriteria criteria) {
        try {
            Map<String, List<String>> options = new HashMap<>();

            // Handle object type hierarchy
            if (criteria.getArtworkType() == null) {
                options.put("objectType", filterOptionsCache.get("classifications", _ ->
                        artworkRepository.findDistinctClassifications().stream()
                                .map(this::getTopLevelCategory)
                                .distinct()
                                .collect(Collectors.toList())
                ));
            } else {
                String selectedType = criteria.getArtworkType();
                validateClassification(selectedType);

                // Get subcategories for selected type
                Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
                List<String> subtypes = artworkRepository.findAll(spec)
                        .stream()
                        .map(Artwork::getClassification)
                        .filter(c -> isSubcategoryOf(c, selectedType))
                        .distinct()
                        .collect(Collectors.toList());

                options.put("subtypes", subtypes);
                options.put("materials", filterOptionsCache.get("mediums", _ ->
                        artworkRepository.findDistinctMediums()));
            }

            // Handle cultural/geographic filtering
            if (criteria.getGeographicLocation() == null) {
                options.put("culturalRegions", CulturalMapping.getCulturalRegions());
                options.put("cultures", filterOptionsCache.get("cultures", _ ->
                        artworkRepository.findDistinctCultures()));
            } else {
                validateGeographicLocation(criteria.getGeographicLocation());
                options.put("cultures",
                        CulturalMapping.getCulturesForRegion(criteria.getGeographicLocation()));
            }

            return options;

        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid filter criteria: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching filter options", e);
            throw new PersistenceException("Failed to fetch filter options", e);
        }
    }

    private void validateClassification(String classification) {
        if (!artworkRepository.findDistinctClassifications()
                .stream()
                .anyMatch(c -> c.startsWith(classification))) {
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

    private boolean isSubcategoryOf(String category, String parentCategory) {
        return category.startsWith(parentCategory + "/");
    }


    public Page<ArtworkDetails> searchArtworks(ArtworkSearchCriteria criteria, Pageable pageable) {
        Page<Artwork> localResults = artworkRepository.findAll(ArtworkSpecifications.withSearchCriteria(criteria), pageable);

        return new PageImpl<>(
                localResults.getContent().stream()
                        .map(this::convertToArtworkDetails)
                        .collect(Collectors.toList()),
                pageable,
                localResults.getTotalElements()
        );
    }

    /**
     * Finds potential artwork candidates for a tour based on basic criteria.
     * This method serves as the first step in tour generation, providing a filtered
     * pool of artworks that meet basic requirements before scoring and selection.
     *
     * @param preferences User's tour preferences including required artworks and filters
     * @param isReturningVisitor Whether this is a returning visitor (affects artwork selection)
     * @return List of artwork candidates that meet basic criteria
     */
    public List<Artwork> findArtworkCandidates(TourPreferences preferences, boolean isReturningVisitor) {
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

        // Add returning visitor criteria if needed
        if (isReturningVisitor) {
            spec = spec.and(ArtworkSpecifications.forReturningVisitor());
        }

        // Add display status requirement - artwork must be on display
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isOnDisplay")));

        // First, get required artworks if any exist
        List<Artwork> candidates = new ArrayList<>();
        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            candidates.addAll(artworkRepository.findAllById(preferences.getRequiredArtworkIds()));

            // Validate all required artworks were found
            if (candidates.size() != preferences.getRequiredArtworkIds().size()) {
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
                .artistName(artwork.getArtist().getArtistName())
                .artistNationality(artwork.getArtist().getNationality())
                .artistBirthYear(artwork.getArtist().getBirthDate())
                .artistDeathYear(artwork.getArtist().getDeathDate())
                .medium(artwork.getMedium())
                .artworkType(artwork.getClassification())
                .currentLocation(artwork.getCurrentLocation())
                .culture(artwork.getCulture())
                .primaryImageUrl(artwork.getImageUrl())
                .isOnView(artwork.getIsOnDisplay())
                .apiSource(artwork.getMuseum() != null ? artwork.getMuseum().getName() : null)
                .tags((List<String>) artwork.getAdditionalMetadata().getOrDefault("tags", new ArrayList<>()))
                .build();
    }

    @Transactional(readOnly = true)
    public ArtworkDetails fetchArtworkById(String id, Long museumId) {
        Museum museum = museumService.findMuseumById(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        return artworkRepository.findByExternalIdAndMuseum(id, museum)
                .map(this::convertToArtworkDetails)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artwork not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Artwork> findAllWithArtistsAndMuseums() {
        return artworkRepository.findAllWithArtistsAndMuseums();
    }

    private void updateArtworkFromDetails(Artwork artwork, ArtworkDetails details) {
        artwork.setTitle(details.getTitle());
        artwork.setMedium(details.getMedium());
        artwork.setImageUrl(details.getPrimaryImageUrl());
        artwork.setDescription(details.getDescription());
        artwork.setCurrentLocation(details.getCurrentLocation());
        artwork.setGalleryNumber(details.getGalleryNumber());
        artwork.setCreationDate(details.getCreationYear());
        artwork.setIsOnDisplay(details.getIsOnView());
        artwork.setArtistPrefix(details.getArtistPrefix());
        artwork.setArtistRole(details.getArtistRole());

        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("tags", new ArrayList<>(details.getTags()));
        additionalMetadata.put("creditLine", details.getCreditLine());
        additionalMetadata.put("additionalImageUrls", new ArrayList<>(details.getAdditionalImageUrls()));

        artwork.setAdditionalMetadata(additionalMetadata);
    }

    public boolean isRecentlyUpdated(String id, Long museumId) {
        Museum museum = museumService.findMuseumById(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        return artworkRepository.findByExternalIdAndMuseum(id, museum)
                .map(artwork -> artwork.getUpdatedAt().isAfter(threshold))
                .orElse(false);
    }

    public List<Artwork> findArtworksNeedingDisplayCheck() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<Artwork> results = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 100); // Process in chunks of 100
        Page<Artwork> page;

        do {
            page = artworkRepository.findByDisplayStatusCheckBefore(threshold, pageable);
            results.addAll(page.getContent());
            pageable = pageable.next();
        } while (page.hasNext());

        return results;
    }

    @Transactional
    public void updateDisplayStatus(Long id, Boolean isOnView) {
        artworkRepository.findById(id)
                .ifPresent(artwork -> {
                    artwork.setIsOnDisplay(isOnView);
                    artwork.setDisplayStatusCheck(LocalDateTime.now());
                    artworkRepository.save(artwork);
                    log.debug("Updated display status for artwork {}: {}", id, isOnView);
                });
    }

    @Transactional
    public void recordProcessingError(String externalId, Long museumId, Exception error) {
        Museum museum = museumService.findMuseumById(museumId)
                .orElseThrow(() -> new ResourceNotFoundException("Museum not found"));

        String errorMessage = error != null ? error.getMessage() : "Unknown error";
        artworkRepository.findByExternalIdAndMuseum(externalId, museum)
                .ifPresent(artwork -> {
                    artwork.setProcessingStatus(Artwork.ProcessingStatus.ERROR);
                    artwork.setLastSyncError(errorMessage);
                    artwork.setLastSyncAttempt(LocalDateTime.now());
                    artworkRepository.save(artwork);
                    log.debug("Recorded processing error for artwork {}: {}", externalId, errorMessage);
                });
    }
}
