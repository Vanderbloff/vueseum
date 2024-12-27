package com.mvp.artplatform.specification;

import com.mvp.artplatform.dto.ArtworkSearchCriteria;
import com.mvp.artplatform.domain.TourPreferences;
import com.mvp.artplatform.entity.Artist;
import com.mvp.artplatform.entity.Artwork;
import com.mvp.artplatform.entity.Museum;
import com.mvp.artplatform.entity.Tour;
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
        return (root, query, cb) -> {
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

            // Add date range predicates
            /*if (criteria.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("creationDate"),
                        criteria.getCreatedAfter()
                ));
            }*/

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
        return (root, query, cb) -> {
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
    public static Specification<Artwork> getThemeSpecificPreFilter(Tour.TourTheme theme) {
        return switch (theme) {
            case CHRONOLOGICAL -> (root, query, cb) -> {
                // For chronological tours, prefer artworks with clear dates
                // and ensure good distribution across time periods
                return cb.and(
                        cb.isNotNull(root.get("creationDate")),
                        cb.notEqual(root.get("creationDate"), "")
                );
            };

            case ARTIST_FOCUSED -> (root, query, cb) -> {
                // For artist-focused tours, prefer works by well-documented artists
                Join<Artwork, Artist> artistJoin = root.join("artist");
                return cb.and(
                        cb.isNotNull(artistJoin.get("birthDate")),
                        cb.isNotNull(artistJoin.get("nationality"))
                );
            };

            case CULTURAL -> (root, query, cb) -> {
                // For cultural tours, ensure we have cultural context
                return cb.isNotNull(root.get("culture"));
            };

            default -> null;
        };
    }

    public static Specification<Artwork> buildSpecificationFromPreferences(TourPreferences prefs) {
        Specification<Artwork> spec = Specification.where(null);

        // Add specifications for each preference if they're not empty
        if (!prefs.getPreferredArtists().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Artwork, Artist> artistJoin = root.join("artist");
                return artistJoin.get("artistName").in(prefs.getPreferredArtists());
            });
        }

        if (!prefs.getPreferredMediums().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("medium").in(prefs.getPreferredMediums()));
        }

        if (!prefs.getPreferredCultures().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("culture").in(prefs.getPreferredCultures()));
        }

        if (!prefs.getPreferredPeriods().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("creationDate").in(prefs.getPreferredPeriods()));
        }

        return spec;
    }

    /*public static Specification<Artwork> forReturningVisitor() {
        return (root, query, cb) -> {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            return cb.or(
                    cb.isNull(root.get("lastViewed")),
                    cb.lessThan(root.get("lastViewed"), threshold)
            );
        };
    }*/

    public static Specification<Artwork> forReturningVisitor() {
        return (root, query, cb) -> cb.isTrue(cb.literal(true));  // Always return true for now
    }

    /**
     * Relaxes constraints when we can't find enough matching artworks.
     * Progressively removes restrictions while maintaining core requirements.
     */
    public static Specification<Artwork> relaxConstraints(
            Specification<Artwork> spec,
            TourPreferences preferences) {

        // Start fresh with only required artworks and display status
        Specification<Artwork> relaxedSpec = Specification.where(null);

        // Keep required artworks
        if (!preferences.getRequiredArtworkIds().isEmpty()) {
            relaxedSpec = relaxedSpec.and((root, query, cb) ->
                    root.get("id").in(preferences.getRequiredArtworkIds()));
        }

        // Always ensure artworks are on display
        relaxedSpec = relaxedSpec.and((root, query, cb) ->
                cb.isTrue(root.get("isOnDisplay")));

        return relaxedSpec;
    }
}
