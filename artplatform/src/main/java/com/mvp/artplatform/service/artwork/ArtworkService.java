package com.mvp.artplatform.service.artwork;

import com.mvp.artplatform.domain.ArtworkDetails;
import com.mvp.artplatform.dto.ArtworkSearchCriteria;
import com.mvp.artplatform.domain.TourPreferences;
import com.mvp.artplatform.entity.Artist;
import com.mvp.artplatform.entity.Artwork;
import com.mvp.artplatform.entity.Museum;
import com.mvp.artplatform.exception.InvalidRequestException;
import com.mvp.artplatform.exception.PersistenceException;
import com.mvp.artplatform.exception.ResourceNotFoundException;
import com.mvp.artplatform.repository.ArtworkRepository;
import com.mvp.artplatform.service.ArtworkSearchService;
import com.mvp.artplatform.service.artist.ArtistService;
import com.mvp.artplatform.service.museum.MuseumService;
import com.mvp.artplatform.specification.ArtworkSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@CacheConfig(cacheNames = "artworks")
public class ArtworkService implements ArtworkSearchService {

    private final ArtworkRepository artworkRepository;
    private final ArtistService artistService;
    private final MuseumService museumService;

    @Cacheable(key = "#details.externalId")
    public Artwork saveFromDetails(ArtworkDetails details) {
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
            return artworkRepository.save(artwork);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("Invalid artwork data: " +
                    e.getMessage());
        }
        catch (PersistenceException e) {
            throw new PersistenceException("Database error while saving artwork: " +
                    e.getMessage());
        }
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
        Specification<Artwork> spec = Objects.requireNonNull(ArtworkSpecifications.getThemeSpecificPreFilter(preferences.getTheme()));

        if (!preferences.getPreferredArtists().isEmpty() ||
                !preferences.getPreferredPeriods().isEmpty() ||
                !preferences.getPreferredMediums().isEmpty()) {
            spec = spec.and(
                    ArtworkSpecifications.forTourPreferences(preferences));
        }

        // Add returning visitor criteria if needed
        if (isReturningVisitor) {
            spec = spec.and(ArtworkSpecifications.forReturningVisitor());
        }

        // Add display status requirement - artwork must be on display
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isOnDisplay")));

        // Get a larger pool of candidates than needed to allow for scoring
        // Multiply maxStops by 3 to ensure enough candidates for good selection
        int candidatePoolSize = preferences.getMaxStops() * 2;

        // First, get required artworks if any exist
        List<Artwork> candidates = new ArrayList<>();
        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            candidates.addAll(artworkRepository.findAllById(preferences.getRequiredArtworkIds()));

            // Validate all required artworks were found
            if (candidates.size() != preferences.getRequiredArtworkIds().size()) {
                throw new InvalidRequestException("Not all required artworks could be found");
            }

            // Reduce the number of additional candidates needed
            candidatePoolSize -= candidates.size();
        }

        // Then get additional candidates
        /*Page<Artwork> additionalCandidates = artworkRepository.findAll(
                spec,
                PageRequest.of(0, candidatePoolSize,
                        Sort.by("lastViewed").ascending()) // Prefer less recently viewed artworks
        );

        candidates.addAll(additionalCandidates.getContent());*/

        // Validate we have enough candidates for minimum tour length
        if (candidates.size() < preferences.getMinStops()) {
            // Try with relaxed constraints
            spec = ArtworkSpecifications.relaxConstraints(spec, preferences);
            Page<Artwork> additionalCandidates = artworkRepository.findAll(
                    spec,
                    PageRequest.of(0, preferences.getMinStops())
            );
            candidates.addAll(additionalCandidates.getContent());

            // If still not enough, throw exception
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

    @Override
    @Transactional(readOnly = true)
    public ArtworkDetails fetchArtworkById(String id) {
        // We'll need to implement this method that was previously
        // delegating to MetMuseumApiClient
        return artworkRepository.findByExternalId(id)
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

        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("tags", new ArrayList<>(details.getTags()));
        additionalMetadata.put("creditLine", details.getCreditLine());
        additionalMetadata.put("additionalImageUrls", new ArrayList<>(details.getAdditionalImageUrls()));

        artwork.setAdditionalMetadata(additionalMetadata);
    }

    public boolean isRecentlyUpdated(String id) {
        return artworkRepository.findByExternalId(id)
                .map(artwork -> {
                    LocalDateTime threshold = LocalDateTime.now().minusDays(1);
                    return artwork.getUpdatedAt().isAfter(threshold);
                })
                .orElse(false);
    }

    public List<Artwork> findArtworksNeedingDisplayCheck() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        return artworkRepository.findByDisplayStatusCheckBefore(threshold);
    }

    @Transactional
    public void updateDisplayStatus(Long id, Boolean isOnView) {
        artworkRepository.findById(id).ifPresent(artwork -> {
            artwork.setIsOnDisplay(isOnView);
            artwork.setDisplayStatusCheck(LocalDateTime.now());
            artworkRepository.save(artwork);
        });
    }

    @Transactional
    public void recordProcessingError(String externalId, Exception error) {
        artworkRepository.findByExternalId(externalId).ifPresent(artwork -> {
            artwork.setProcessingStatus(Artwork.ProcessingStatus.ERROR);
            artwork.setLastSyncError(error.getMessage());
            artwork.setLastSyncAttempt(LocalDateTime.now());
            artworkRepository.save(artwork);
        });
    }
}
