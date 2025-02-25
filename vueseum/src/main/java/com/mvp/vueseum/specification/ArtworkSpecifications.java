package com.mvp.vueseum.specification;

import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.service.cultural.CulturalMapping;
import com.mvp.vueseum.util.DateParsingUtil;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ArtworkSpecifications {

    public static Specification<Artwork> withSearchCriteria(ArtworkSearchCriteria criteria) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Has Image filter
            if (criteria.getHasImage() != null && criteria.getHasImage()) {
                predicates.add(cb.or(
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
                ));
            }

            // Era filter
            if (criteria.getPeriod() != null && !criteria.getPeriod().isEmpty()) {
                String periodStr = criteria.getPeriod();
                try {
                    // Special case handling for specific period formats
                    if (periodStr.equals("1000 B.C.-A.D. 1")) {
                        Expression<String> creationDate = root.get("creationDate");
                        predicates.add(cb.and(
                                cb.isNotNull(creationDate),
                                cb.between(
                                        cb.function(
                                                "extract_year_from_date",
                                                Integer.class,
                                                creationDate
                                        ),
                                        -1000, // 1000 B.C.
                                        1      // A.D. 1
                                )
                        ));
                    }
                    else if (periodStr.equals("2000-1000 B.C.")) {
                        Expression<String> creationDate = root.get("creationDate");
                        predicates.add(cb.and(
                                cb.isNotNull(creationDate),
                                cb.between(
                                        cb.function(
                                                "extract_year_from_date",
                                                Integer.class,
                                                creationDate
                                        ),
                                        -2000, // 2000 B.C.
                                        -1000  // 1000 B.C.
                                )
                        ));
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
                                log.warn("Failed to parse start year: {}", rangeParts[0]);
                                throw e;
                            }

                            if (rangeParts[1].trim().equals("present")) {
                                endYear = java.time.Year.now().getValue(); // Current year
                            } else {
                                try {
                                    endYear = Integer.parseInt(rangeParts[1].trim());
                                } catch (NumberFormatException e) {
                                    log.warn("Failed to parse end year: {}", rangeParts[1]);
                                    throw e;
                                }
                            }

                            log.info("A.D. Period filter: parsed years {} to {}", startYear, endYear);

                            Expression<String> creationDate = root.get("creationDate");
                            predicates.add(cb.and(
                                    cb.isNotNull(creationDate),
                                    cb.between(
                                            cb.function(
                                                    "extract_year_from_date",
                                                    Integer.class,
                                                    creationDate
                                            ),
                                            startYear,
                                            endYear
                                    )
                            ));
                        }
                    } else {
                        String[] parts = periodStr.split("-");
                        if (parts.length == 2) {
                            String startPeriod = parts[0].trim();
                            String endPeriod = parts[1].trim();

                            int startYear = DateParsingUtil.extractYear(startPeriod);
                            int endYear = DateParsingUtil.extractYear(endPeriod);

                            Expression<String> creationDate = root.get("creationDate");
                            predicates.add(cb.and(
                                    cb.isNotNull(creationDate),
                                    cb.between(
                                            cb.function(
                                                    "extract_year_from_date",
                                                    Integer.class,
                                                    creationDate
                                            ),
                                            startYear,
                                            endYear
                                    )
                            ));
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse era period: {}", periodStr, e);
                }
            }

            // Artwork Type filter
            if (criteria.getArtworkType() != null && !criteria.getArtworkType().isEmpty()) {
                predicates.add(cb.equal(
                        root.get("classification"),
                        criteria.getArtworkType()
                ));
            }

            // Handle medium separately
            if (criteria.getMedium() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("medium")),
                                "%" + criteria.getMedium().toLowerCase() + "%"
                        )
                );
            }

            // Handle geographic and cultural relationships
            if (criteria.getCulture() != null) {
                // Direct culture match takes priority
                predicates.add(cb.equal(
                        cb.lower(root.get("culture")),
                        criteria.getCulture().toLowerCase()
                ));
            } else if (criteria.getGeographicLocation() != null) {
                // If no direct culture specified, use geographic location with mapping
                List<String> cultures = CulturalMapping
                        .getCulturesForRegion(criteria.getGeographicLocation());

                predicates.add(cb.or(
                        // Direct geographic location match
                        cb.equal(
                                cb.lower(root.get("culture")),
                                criteria.getGeographicLocation().toLowerCase()
                        ),
                        // Related cultures from mapping
                        root.get("culture").in(cultures)
                ));
            }

            if (criteria.getSortField() != null &&
                    criteria.getSortField().equals("artist")) {
                root.fetch("artist", JoinType.LEFT);
            }

            // Title search with partial matching
            if (StringUtils.hasText(criteria.getTitle())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + criteria.getTitle().toLowerCase() + "%"
                        )
                );
            }

            // Artist name search with partial matching
            if (StringUtils.hasText(criteria.getArtistName())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("artist").get("artistName")),
                                "%" + criteria.getArtistName().toLowerCase() + "%"
                        )
                );
            }

            // Combine all predicates with AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Artwork> forTourPreferences(TourPreferences preferences) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
                predicates.add(root.get("period").in(preferences.getPreferredPeriods()));
            }

            if (preferences.getTheme() != null) {
                predicates.add(root.get("theme").in(preferences.getTheme()));
                //predicates.add(cb.equal(root.get("theme"), preferences.getTheme()));
            }

            if (preferences.getPreferredCultures() != null) {
                Predicate culturePredicate = root.get("culture").in(preferences.getPreferredCultures());

                Predicate locationPredicate = preferences.getPreferredCultures().stream()
                        .flatMap(culture -> CulturalMapping.getCountriesForCulture(culture, true).stream())
                        .map(country -> cb.equal(root.get("geographicLocation"), country))
                        .reduce(cb::or)
                        .orElse(cb.conjunction());

                // Combine predicates with OR - match either culture directly or via location
                return cb.or(culturePredicate, locationPredicate);
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

            return cb.and(museumPredicate, themePredicate);
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
            spec = spec.and((root, _, _) ->
                    root.get("creationDate").in(prefs.getPreferredPeriods()));
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
        // Start with theme requirements
        Specification<Artwork> core = getThemeSpecificPreFilter(preferences.getTheme(), preferences.getMuseumId());

        // Add display status check
        core = core.and((root, _, cb) -> cb.isTrue(root.get("isOnDisplay")));

        core = core.and((root, _, cb) -> {
            Join<Artwork, Museum> museumJoin = root.join("museum");
            return cb.equal(museumJoin.get("id"), preferences.getMuseumId());
        });

        // Add required artworks if any
        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            core = core.and((root, _, _) ->
                    root.get("id").in(preferences.getRequiredArtworkIds()));
        }

        return core;
    }
}
