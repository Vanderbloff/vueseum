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

    // Keep existing count methods for backward compatibility
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

    // Keep existing distinct methods for backward compatibility
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

    @Query("SELECT a.classification as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.classification IS NOT NULL " +
            "GROUP BY a.classification " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findClassificationsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.medium as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.medium IS NOT NULL " +
            "GROUP BY a.medium " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findMediumsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.country as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.country IS NOT NULL " +
            "GROUP BY a.country " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findGeographicLocationsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.region as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.region IS NOT NULL " +
            "GROUP BY a.region " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findRegionsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.culture as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.culture IS NOT NULL " +
            "GROUP BY a.culture " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findCulturesWithCountsLimited(@Param("limit") int limit);

    @Query(
            value = "SELECT * FROM artworks a " +
                    "WHERE ((:hasImage = false) OR (a.image_url IS NOT NULL AND LENGTH(a.image_url) > 0)) " +
                    "AND (:title IS NULL OR LOWER(a.title) LIKE CONCAT('%', LOWER(:title), '%')) " +
                    "AND (:origin IS NULL OR LOWER(a.culture) LIKE CONCAT('%', LOWER(:origin), '%') " +
                    "    OR LOWER(a.country) LIKE CONCAT('%', LOWER(:origin), '%')) " +
                    "AND (:category IS NULL OR LOWER(a.classification) LIKE CONCAT('%', LOWER(:category), '%') " +
                    "    OR LOWER(a.medium) LIKE CONCAT('%', LOWER(:category), '%')) " +
                    "ORDER BY extract_year_from_date(a.creation_date) DESC NULLS LAST",
            countQuery = "SELECT COUNT(*) FROM artworks a " +
                    "WHERE ((:hasImage = false) OR (a.image_url IS NOT NULL AND LENGTH(a.image_url) > 0)) " +
                    "AND (:title IS NULL OR LOWER(a.title) LIKE CONCAT('%', LOWER(:title), '%')) " +
                    "AND (:origin IS NULL OR LOWER(a.culture) LIKE CONCAT('%', LOWER(:origin), '%') " +
                    "    OR LOWER(a.country) LIKE CONCAT('%', LOWER(:origin), '%')) " +
                    "AND (:category IS NULL OR LOWER(a.classification) LIKE CONCAT('%', LOWER(:category), '%') " +
                    "    OR LOWER(a.medium) LIKE CONCAT('%', LOWER(:category), '%'))",
            nativeQuery = true)
    Page<Artwork> findWithDateSortDesc(
            @Param("hasImage") boolean hasImage,
            @Param("title") String title,
            @Param("origin") String origin,
            @Param("category") String category,
            Pageable pageable);


    @Query(
            value = "SELECT * FROM artworks a " +
                    "WHERE ((:hasImage = false) OR (a.image_url IS NOT NULL AND LENGTH(a.image_url) > 0)) " +
                    "AND (:title IS NULL OR LOWER(a.title) LIKE CONCAT('%', LOWER(:title), '%')) " +
                    "AND (:origin IS NULL OR LOWER(a.culture) LIKE CONCAT('%', LOWER(:origin), '%') " +
                    "    OR LOWER(a.country) LIKE CONCAT('%', LOWER(:origin), '%')) " +
                    "AND (:category IS NULL OR LOWER(a.classification) LIKE CONCAT('%', LOWER(:category), '%') " +
                    "    OR LOWER(a.medium) LIKE CONCAT('%', LOWER(:category), '%')) " +
                    "ORDER BY extract_year_from_date(a.creation_date) ASC NULLS LAST",
            countQuery = "SELECT COUNT(*) FROM artworks a " +
                    "WHERE ((:hasImage = false) OR (a.image_url IS NOT NULL AND LENGTH(a.image_url) > 0)) " +
                    "AND (:title IS NULL OR LOWER(a.title) LIKE CONCAT('%', LOWER(:title), '%')) " +
                    "AND (:origin IS NULL OR LOWER(a.culture) LIKE CONCAT('%', LOWER(:origin), '%') " +
                    "    OR LOWER(a.country) LIKE CONCAT('%', LOWER(:origin), '%')) " +
                    "AND (:category IS NULL OR LOWER(a.classification) LIKE CONCAT('%', LOWER(:category), '%') " +
                    "    OR LOWER(a.medium) LIKE CONCAT('%', LOWER(:category), '%'))",
            nativeQuery = true)
    Page<Artwork> findWithDateSortAsc(
            @Param("hasImage") boolean hasImage,
            @Param("title") String title,
            @Param("origin") String origin,
            @Param("category") String category,
            Pageable pageable);

    List<Artwork> findByTitleContainingAndMuseumId(String titleFragment, Long museumId);

    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);
}