package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.specification.ArtworkSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FilterOptionsService {
    private final ArtworkRepository artworkRepository;
    private final Cache<String, FilterOptions> filterOptionsCache;

    public record FilterOptions(
            Set<String> availableArtists,
            Set<String> availableMediums,
            Set<String> availableCultures,
            Set<String> availablePeriods
    ) {}

    public FilterOptionsService(ArtworkRepository artworkRepository) {
        this.artworkRepository = artworkRepository;
        this.filterOptionsCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(1000)
                .build();
    }

    public FilterOptions getAvailableOptions(TourPreferences prefs, Long museumId) {
        if (museumId == null) {
            throw new IllegalArgumentException("Museum ID cannot be null");
        }

        String cacheKey = generateCacheKey(prefs, museumId);
        return filterOptionsCache.get(cacheKey, k -> computeFilterOptions(prefs, museumId));
    }

    private FilterOptions computeFilterOptions(TourPreferences prefs, Long museumId) {
        Specification<Artwork> spec = ArtworkSpecifications.buildSpecificationFromPreferences(prefs)
                .and((root, query, cb) -> cb.equal(root.get("museum").get("id"), museumId))
                .and((root, query, cb) -> cb.isTrue(root.get("isOnDisplay")));

        List<Artwork> matchingArtworks = artworkRepository.findAll(spec);

        return new FilterOptions(
                extractUniqueValues(matchingArtworks, Artwork::getArtistName),
                extractUniqueValues(matchingArtworks, Artwork::getMedium),
                extractUniqueValues(matchingArtworks, Artwork::getCulture),
                extractUniqueValues(matchingArtworks, Artwork::getCreationDate)
        );
    }

    private <T> Set<T> extractUniqueValues(List<Artwork> artworks, Function<Artwork, T> extractor) {
        return artworks.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String generateCacheKey(TourPreferences prefs, Long museumId) {
        return String.format("%d-%s-%s-%s-%s",
                museumId,
                prefs.getPreferredArtists().hashCode(),
                prefs.getPreferredMediums().hashCode(),
                prefs.getPreferredCultures().hashCode(),
                prefs.getPreferredPeriods().hashCode()
        );
    }
}
