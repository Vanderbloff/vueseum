package com.mvp.vueseum.repository;

import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByExternalIdAndMuseum(String externalId, Museum museum);

    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.artist " +
            "LEFT JOIN FETCH a.museum")
    List<Artwork> findAllWithArtistsAndMuseums();

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.museum.id = :museumId")
    long countByMuseum(Long museumId);

    // Original individual count methods - (kept for backward compatibility)
    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.classification = :classification")
    long countByClassification(@Param("classification") String classification);

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.country = :geographicLocation")
    long countByGeographicLocation(@Param("geographicLocation") String geographicLocation);

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.medium = :medium")
    long countByMedium(@Param("medium") String medium);

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.region = :region")
    long countByRegion(@Param("region") String region);

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.culture = :culture")
    long countByCulture(@Param("culture") String culture);

    // Original unlimited distinct queries - (kept for backward compatibility)
    @Query("SELECT DISTINCT a.classification FROM Artwork a WHERE a.classification IS NOT NULL ORDER BY a.classification")
    List<String> findDistinctClassifications();

    @Query("SELECT DISTINCT a.country FROM Artwork a WHERE a.country IS NOT NULL ORDER BY a.country")
    List<String> findDistinctGeographicLocations();

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediums();

    @Query("SELECT DISTINCT a.region FROM Artwork a WHERE a.region IS NOT NULL ORDER BY a.region")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.culture IS NOT NULL ORDER BY a.culture")
    List<String> findDistinctCultures();

    @Query("SELECT DISTINCT a.classification FROM Artwork a WHERE a.classification IS NOT NULL ORDER BY a.classification LIMIT :limit")
    List<String> findDistinctClassificationsLimited(@Param("limit") int limit);

    @Query("SELECT DISTINCT a.country FROM Artwork a WHERE a.country IS NOT NULL ORDER BY a.country LIMIT :limit")
    List<String> findDistinctGeographicLocationsLimited(@Param("limit") int limit);

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium LIMIT :limit")
    List<String> findDistinctMediumsLimited(@Param("limit") int limit);

    @Query("SELECT DISTINCT a.region FROM Artwork a WHERE a.region IS NOT NULL ORDER BY a.region LIMIT :limit")
    List<String> findDistinctRegionsLimited(@Param("limit") int limit);

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.culture IS NOT NULL ORDER BY a.culture LIMIT :limit")
    List<String> findDistinctCulturesLimited(@Param("limit") int limit);

    @Query("SELECT a.classification, COUNT(a) FROM Artwork a WHERE a.classification IS NOT NULL GROUP BY a.classification")
    List<Object[]> countByClassificationGrouped();

    @Query("SELECT a.country, COUNT(a) FROM Artwork a WHERE a.country IS NOT NULL GROUP BY a.country")
    List<Object[]> countByGeographicLocationGrouped();

    @Query("SELECT a.medium, COUNT(a) FROM Artwork a WHERE a.medium IS NOT NULL GROUP BY a.medium")
    List<Object[]> countByMediumGrouped();

    @Query("SELECT a.region, COUNT(a) FROM Artwork a WHERE a.region IS NOT NULL GROUP BY a.region")
    List<Object[]> countByRegionGrouped();

    @Query("SELECT a.culture, COUNT(a) FROM Artwork a WHERE a.culture IS NOT NULL GROUP BY a.culture")
    List<Object[]> countByCultureGrouped();

    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);
}