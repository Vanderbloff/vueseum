package com.mvp.vueseum.specification;

import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.service.cultural.CulturalMapping;
import com.mvp.vueseum.util.DateParsingUtil;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ArtworkSpecifications {

    /**
     * Creates a predicate requiring an artwork to have an image
     */
    public static Predicate createHasImagePredicate(Root<Artwork> root, CriteriaBuilder cb) {
        return cb.or(
                // Check primary image
                cb.and(
                        cb.isNotNull(root.get("imageUrl")),
                        cb.notEqual(root.get("imageUrl"), "")
                ),
                // Also check thumbnail image
                cb.and(
                        cb.isNotNull(root.get("thumbnailImageUrl")),
                        cb.notEqual(root.get("thumbnailImageUrl"), "")
                )
        );
    }

    /**
     * Creates a predicate for matching artworks by category (classification or medium)
     */
    private static Predicate createCategoryPredicate(String category, Root<Artwork> root, CriteriaBuilder cb) {
        if (!StringUtils.hasText(category)) {
            return null;
        }

        String categoryTerm = category.toLowerCase();
        return cb.or(
                cb.like(cb.lower(root.get("classification")), "%" + categoryTerm + "%"),
                cb.like(cb.lower(root.get("medium")), "%" + categoryTerm + "%")
        );
    }

    /**
     * Creates a predicate for matching artworks by origin (culture, country, region)
     */
    private static Predicate createOriginPredicate(String origin, Root<Artwork> root, CriteriaBuilder cb) {
        if (!StringUtils.hasText(origin)) {
            return null;
        }

        String originTerm = origin.toLowerCase().trim();
        log.debug("Processing origin filter with term: {}", originTerm);

        try {
            // Basic text matching across all fields
            Predicate basicMatch = cb.or(
                    cb.like(cb.lower(root.get("culture")), "%" + originTerm + "%"),
                    cb.like(cb.lower(root.get("country")), "%" + originTerm + "%"),
                    cb.like(cb.lower(root.get("region")), "%" + originTerm + "%")
            );

            // Also try cultural mapping as an enhancement
            try {
                List<String> relatedCultures = CulturalMapping.getCulturesForRegion(originTerm);
                if (!relatedCultures.isEmpty()) {
                    Predicate culturalMatch = root.get("culture").in(relatedCultures);
                    return cb.or(basicMatch, culturalMatch);
                }
            } catch (Exception e) {
                // Log but don't fail if cultural mapping fails
                log.warn("Cultural mapping failed for term: {}, error: {}", originTerm, e.getMessage());
            }

            return basicMatch;
        } catch (Exception e) {
            // Log the specific error but return a non-restricting predicate
            log.error("Error creating origin filter predicate: {}", e.getMessage());
            return cb.conjunction();
        }
    }

    /**
     * Creates a predicate for title search
     */
    private static Predicate createTitlePredicate(String title, Root<Artwork> root, CriteriaBuilder cb) {
        if (!StringUtils.hasText(title)) {
            return null;
        }

        return cb.like(
                cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%"
        );
    }

    /**
     * Creates a predicate for artist name search
     */
    private static Predicate createArtistPredicate(String artistName, Root<Artwork> root, CriteriaBuilder cb) {
        if (!StringUtils.hasText(artistName)) {
            return null;
        }

        return cb.like(
                cb.lower(root.get("artist").get("artistName")),
                "%" + artistName.toLowerCase() + "%"
        );
    }

    /**
     * Creates a predicate for matching artwork creation dates to a period range.
     * @param periodStr The period string (e.g., "A.D. 1400-1600", "1000 B.C.-A.D. 1")
     * @param creationDateExpr The expression for the creation date field
     * @param cb The criteria builder
     * @return A predicate for matching artwork to the period
     */
    private static Predicate createDateRangePredicate(
            String periodStr,
            Expression<String> creationDateExpr,
            CriteriaBuilder cb) {

        try {
            // Special case handling for specific period formats
            if (periodStr.equals("1000 B.C.-A.D. 1")) {
                return cb.and(
                        cb.isNotNull(creationDateExpr),
                        cb.between(
                                cb.function(
                                        "extract_year_from_date",
                                        Integer.class,
                                        creationDateExpr
                                ),
                                -1000, // 1000 B.C.
                                1      // A.D. 1
                        )
                );
            }
            else if (periodStr.equals("2000-1000 B.C.")) {
                return cb.and(
                        cb.isNotNull(creationDateExpr),
                        cb.between(
                                cb.function(
                                        "extract_year_from_date",
                                        Integer.class,
                                        creationDateExpr
                                ),
                                -2000, // 2000 B.C.
                                -1000  // 1000 B.C.
                        )
                );
            }
            else if (periodStr.startsWith("A.D.")) {
                // Handle A.D. ranges
                String yearPart = periodStr.substring(4).trim(); // Remove "A.D. "
                String[] rangeParts = yearPart.split("-");

                if (rangeParts.length == 2) {
                    int startYear;
                    int endYear;

                    try {
                        startYear = Integer.parseInt(rangeParts[0].trim());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse start year: {}", rangeParts[0], e);
                        return cb.conjunction();
                    }

                    if (rangeParts[1].trim().equals("present")) {
                        endYear = java.time.Year.now().getValue();
                    } else {
                        try {
                            endYear = Integer.parseInt(rangeParts[1].trim());
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse end year: {}", rangeParts[1], e);
                            return cb.conjunction();
                        }
                    }

                    log.debug("Parsed A.D. period: {} to {}", startYear, endYear);

                    return cb.and(
                            cb.isNotNull(creationDateExpr),
                            cb.between(
                                    cb.function(
                                            "extract_year_from_date",
                                            Integer.class,
                                            creationDateExpr
                                    ),
                                    startYear,
                                    endYear
                            )
                    );
                }
            }
            else {
                String[] parts = periodStr.split("-");
                if (parts.length == 2) {
                    String startPeriod = parts[0].trim();
                    String endPeriod = parts[1].trim();

                    int startYear = DateParsingUtil.extractYear(startPeriod);
                    int endYear = DateParsingUtil.extractYear(endPeriod);

                    log.debug("Parsed period range: {} to {}", startYear, endYear);

                    return cb.and(
                            cb.isNotNull(creationDateExpr),
                            cb.between(
                                    cb.function(
                                            "extract_year_from_date",
                                            Integer.class,
                                            creationDateExpr
                                    ),
                                    startYear,
                                    endYear
                            )
                    );
                }
            }

            log.warn("Could not parse period format: {}", periodStr);
            return cb.conjunction(); // Return TRUE predicate if parsing fails
        } catch (Exception e) {
            log.error("Error creating date range predicate for period {}: {}",
                    periodStr, e.getMessage());
            return cb.conjunction(); // Return a TRUE predicate that doesn't filter anything
        }
    }

    public static Specification<Artwork> withSearchCriteria(ArtworkSearchCriteria criteria) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Has Image filter
            if (criteria.getHasImage() != null && criteria.getHasImage()) {
                predicates.add(createHasImagePredicate(root, cb));
            }

            // Era filter
            if (StringUtils.hasText(criteria.getPeriod())) {
                Expression<String> creationDate = root.get("creationDate");
                Predicate periodPredicate = createDateRangePredicate(criteria.getPeriod(), creationDate, cb);
                predicates.add(periodPredicate);
            }

            // Category filter
            Predicate categoryPredicate = createCategoryPredicate(criteria.getCategory(), root, cb);
            if (categoryPredicate != null) {
                predicates.add(categoryPredicate);
            }

            // Origin filter
            Predicate originPredicate = createOriginPredicate(criteria.getOrigin(), root, cb);
            if (originPredicate != null) {
                predicates.add(originPredicate);
            }

            // Handle sort fields that require joins
            if (criteria.getSortField() != null &&
                    criteria.getSortField().equals("artist")) {
                root.join("artist", JoinType.LEFT);
            }

            // Title search
            Predicate titlePredicate = createTitlePredicate(criteria.getTitle(), root, cb);
            if (titlePredicate != null) {
                predicates.add(titlePredicate);
            }

            // Artist name search
            Predicate artistPredicate = createArtistPredicate(criteria.getArtistName(), root, cb);
            if (artistPredicate != null) {
                predicates.add(artistPredicate);
            }

            // Combine all predicates with AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Artwork> forTourPreferences(TourPreferences preferences) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(createHasImagePredicate(root, cb));

            if (!preferences.getRequiredArtworkIds().isEmpty()) {
                predicates.add(root.get("id").in(preferences.getRequiredArtworkIds()));
            }

            if (!preferences.getPreferredArtists().isEmpty()) {
                Join<Artwork, Artist> artistJoin = root.join("artist");
                predicates.add(artistJoin.get("artistName")
                        .in(preferences.getPreferredArtists()));
            }

            if (!preferences.getPreferredMediums().isEmpty()) {
                predicates.add(root.get("medium").in(preferences.getPreferredMediums()));
            }

            if (!preferences.getPreferredPeriods().isEmpty()) {
                String periodStr = preferences.getPreferredPeriods().iterator().next();
                Expression<String> creationDate = root.get("creationDate");
                predicates.add(createDateRangePredicate(periodStr, creationDate, cb));
            }

            if (preferences.getPreferredCultures() != null && !preferences.getPreferredCultures().isEmpty()) {
                Predicate culturePredicate = root.get("culture").in(preferences.getPreferredCultures());

                Predicate locationPredicate = preferences.getPreferredCultures().stream()
                        .flatMap(culture -> CulturalMapping.getCountriesForCulture(culture, true).stream())
                        .map(country -> cb.equal(root.get("country"), country))
                        .reduce(cb::or)
                        .orElse(cb.conjunction());

                // Combine predicates with OR - match either culture directly or via location
                predicates.add(cb.or(culturePredicate, locationPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Theme-specific criteria to reduce the initial candidate pool
     * before detailed scoring
     */
    public static Specification<Artwork> getThemeSpecificPreFilter(Tour.TourTheme theme, Long museumId) {
        return (root, _, cb) -> {
            // First add the museum filter
            Join<Artwork, Museum> museumJoin = root.join("museum");
            Predicate museumPredicate = cb.equal(museumJoin.get("id"), museumId);

            Predicate hasImagePredicate = createHasImagePredicate(root, cb);

            // Then add theme-specific filters
            Predicate themePredicate = switch (theme) {
                case CHRONOLOGICAL -> cb.and(
                        cb.isNotNull(root.get("creationDate")),
                        cb.notEqual(root.get("creationDate"), "")
                );
                case ARTIST_FOCUSED -> {
                    Join<Artwork, Artist> artistJoin = root.join("artist");
                    yield cb.and(
                            cb.isNotNull(artistJoin.get("birthDate")),
                            cb.isNotNull(artistJoin.get("nationality"))
                    );
                }
                case CULTURAL -> cb.isNotNull(root.get("culture"));
            };

            return cb.and(museumPredicate, hasImagePredicate, themePredicate);
        };
    }

    public static Specification<Artwork> buildSpecificationFromPreferences(TourPreferences prefs) {
        Specification<Artwork> spec = Specification.where(null);

        // Add specifications for each preference if they're not empty
        if (!prefs.getPreferredArtists().isEmpty()) {
            spec = spec.and((root, _, _) -> {
                Join<Artwork, Artist> artistJoin = root.join("artist");
                return artistJoin.get("artistName").in(prefs.getPreferredArtists());
            });
        }

        if (!prefs.getPreferredMediums().isEmpty()) {
            spec = spec.and((root, _, _) ->
                    root.get("medium").in(prefs.getPreferredMediums()));
        }

        if (!prefs.getPreferredCultures().isEmpty()) {
            spec = spec.and((root, _, _) ->
                    root.get("culture").in(prefs.getPreferredCultures()));
        }

        if (!prefs.getPreferredPeriods().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                String periodStr = prefs.getPreferredPeriods().iterator().next();
                Expression<String> creationDate = root.get("creationDate");
                return createDateRangePredicate(periodStr, creationDate, cb);
            });
        }

        return spec;
    }

    /**
     * Relaxes constraints when we can't find enough matching artworks.
     * Progressively removes restrictions while maintaining core requirements.
     */
    public static List<Specification<Artwork>> relaxConstraints(
            Specification<Artwork> originalSpec,
            TourPreferences preferences) {

        List<Specification<Artwork>> relaxationLevels = new ArrayList<>();
        relaxationLevels.add(originalSpec);

        // Build core specification that's common across all relaxation levels
        Specification<Artwork> coreSpec = buildCoreSpecification(preferences);

        // Level 1: Core + Artist preferences
        if (!preferences.getPreferredArtists().isEmpty()) {
            Specification<Artwork> withArtists = coreSpec.and((root, _, _) -> {
                Join<Artwork, Artist> artistJoin = root.join("artist");
                return artistJoin.get("artistName").in(preferences.getPreferredArtists());
            });
            relaxationLevels.add(withArtists);
        }

        // Level 2: Core requirements only
        relaxationLevels.add(coreSpec);

        return relaxationLevels;
    }

    /**
     * Builds the core specification that must be maintained across all relaxation levels
     */
    private static Specification<Artwork> buildCoreSpecification(TourPreferences preferences) {
        Specification<Artwork> core = getThemeSpecificPreFilter(preferences.getTheme(), preferences.getMuseumId());

        core = core.and((root, _, cb) -> cb.isTrue(root.get("isOnDisplay")));

        core = core.and((root, _, cb) -> createHasImagePredicate(root, cb));

        core = core.and((root, _, cb) -> {
            Join<Artwork, Museum> museumJoin = root.join("museum");
            return cb.equal(museumJoin.get("id"), preferences.getMuseumId());
        });

        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            core = core.and((root, _, _) ->
                    root.get("id").in(preferences.getRequiredArtworkIds()));
        }

        return core;
    }
}
