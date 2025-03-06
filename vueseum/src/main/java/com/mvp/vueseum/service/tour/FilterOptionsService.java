package com.mvp.vueseum.service.tour;

import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.specification.ArtworkSpecifications;
import com.mvp.vueseum.util.DateParsingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterOptionsService {
    private final ArtworkRepository artworkRepository;
    private final Cache<String, FilterOptions> filterOptionsCache;

    public record FilterOptions(
            Set<String> availableMediums,
            Set<String> availableCultures,
            Set<String> availablePeriods
    ) {}

    public FilterOptions getAvailableOptions(TourPreferences prefs, Long museumId) {
        if (museumId == null) {
            throw new IllegalArgumentException("Museum ID cannot be null");
        }

        String cacheKey = generateCacheKey(prefs, museumId);
        return filterOptionsCache.get(cacheKey, _ -> computeFilterOptions(prefs, museumId));
    }

    private FilterOptions computeFilterOptions(TourPreferences prefs, Long museumId) {
        Specification<Artwork> spec = ArtworkSpecifications.buildSpecificationFromPreferences(prefs)
                .and((root, _, cb) -> cb.equal(root.get("museum").get("id"), museumId));

        spec = spec.and((root, _, cb) ->
                cb.or(
                        cb.and(
                                cb.isNotNull(root.get("imageUrl")),
                                cb.notEqual(root.get("imageUrl"), "")
                        ),
                        cb.and(
                                cb.isNotNull(root.get("thumbnailImageUrl")),
                                cb.notEqual(root.get("thumbnailImageUrl"), "")
                        )
                )
        );

        List<Artwork> matchingArtworks = artworkRepository.findAll(spec);

        return new FilterOptions(
                extractUniqueValues(matchingArtworks, Artwork::getMedium),
                extractUniqueValues(matchingArtworks, Artwork::getCulture),
                extractPeriodValues(matchingArtworks)
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

    private Set<String> extractPeriodValues(List<Artwork> artworks) {
        return artworks.stream()
                .map(Artwork::getCreationDate)
                .filter(Objects::nonNull)
                .map(date -> {
                    try {
                        int year = DateParsingUtil.extractYear(date);
                        return DateParsingUtil.mapYearToPeriod(year);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
