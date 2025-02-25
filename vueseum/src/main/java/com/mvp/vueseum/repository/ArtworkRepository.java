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

    // Classification/Type and Medium
    @Query("SELECT DISTINCT a.classification FROM Artwork a WHERE a.classification IS NOT NULL ORDER BY a.classification")
    List<String> findDistinctClassifications();

    // Geographic Location
    @Query("SELECT DISTINCT a.country FROM Artwork a WHERE a.country IS NOT NULL ORDER BY a.country")
    List<String> findDistinctGeographicLocations();

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediums();

    @Query("SELECT DISTINCT a.region FROM Artwork a WHERE a.region IS NOT NULL ORDER BY a.region")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.culture IS NOT NULL ORDER BY a.culture")
    List<String> findDistinctCultures();

    // Additional query helpers
    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);


    // To be used in future for hierarchical querying
    /*@Query("SELECT COUNT(a) FROM Artwork a WHERE a.region = :region AND a.country = :geographicLocation")
    long countByRegionAndGeographicLocation(
            @Param("region") String region,
            @Param("geographicLocation") String geographicLocation
    );

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.culture = :culture AND a.region = :region")
    long countByCultureAndRegion(
            @Param("culture") String culture,
            @Param("region") String region
    );

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.medium = :medium AND a.classification = :classification")
    long countByMediumAndClassification(
            @Param("medium") String medium,
            @Param("classification") String classification
    );

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.classification = :classification AND a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediumsByClassification(@Param("classification") String classification);

    @Query("SELECT DISTINCT a.region FROM Artwork a WHERE a.country = :location AND a.region IS NOT NULL ORDER BY a.region")
    List<String> findDistinctRegionsByLocation(@Param("location") String location);

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.region = :region AND a.culture IS NOT NULL ORDER BY a.culture")
    List<String> findDistinctCulturesByRegion(@Param("region") String region);*/
}