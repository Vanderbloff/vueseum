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
    // Base find methods
    Optional<Artwork> findByExternalIdAndMuseum(String externalId, Museum museum);
    List<Artwork> findAllWithArtistsAndMuseums();
    long countByMuseum(Long museumId);

    // Classification/Type and Medium
    @Query("SELECT DISTINCT a.classification FROM Artwork a WHERE a.classification IS NOT NULL ORDER BY a.classification")
    List<String> findDistinctClassifications();

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediums();

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.classification = :classification AND a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediumsByClassification(@Param("classification") String classification);

    // Geographic Location hierarchy
    @Query("SELECT DISTINCT a.geographicLocation FROM Artwork a WHERE a.geographicLocation IS NOT NULL ORDER BY a.geographicLocation")
    List<String> findDistinctGeographicLocations();

    @Query("SELECT DISTINCT a.region FROM Artwork a WHERE a.geographicLocation = :location AND a.region IS NOT NULL ORDER BY a.region")
    List<String> findDistinctRegionsByLocation(@Param("location") String location);

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.region = :region AND a.culture IS NOT NULL ORDER BY a.culture")
    List<String> findDistinctCulturesByRegion(@Param("region") String region);

    // Additional query helpers
    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);
}