package com.mvp.vueseum.specification;

import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArtworkSpecifications {
    public static Specification<Artwork> withSearchCriteria(ArtworkSearchCriteria criteria) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getTitle() != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + criteria.getTitle().toLowerCase() + "%"
                ));
            }

            if (criteria.getArtistName() != null) {
                Join<Artwork, Artist> artistJoin = root.join("artist", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(artistJoin.get("artistName")),
                        "%" + criteria.getArtistName().toLowerCase() + "%"
                ));
            }

            if (criteria.getMedium() != null) {
                predicates.add(cb.equal(root.get("medium"), criteria.getMedium()));
            }

            // Handle museum filtering for future multi-museum support
            if (criteria.getMuseums() != null && !criteria.getMuseums().isEmpty()) {
                Join<Artwork, Museum> museumJoin = root.join("museum");
                predicates.add(museumJoin.get("name").in(criteria.getMuseums()));

                if (criteria.getDepartment() != null) {
                    predicates.add(cb.equal(museumJoin.get("department"), criteria.getDepartment()));
                }
            }

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
                predicates.add(root.get("culture").in(preferences.getPreferredCultures()));
                //predicates.add(cb.equal(root.get("culture"), preferences.getPreferredCultures()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Theme-specific criteria to reduce the initial candidate pool
     * before detailed scoring
     */
    public static Specification<Artwork> getThemeSpecificPreFilter(Tour.TourTheme theme, Long museumId) {
        return (root, query, cb) -> {
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

    public static Specification<Artwork> forReturningVisitor() {
        return (_, _, cb) -> cb.isTrue(cb.literal(true));  // Always return true for now
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
