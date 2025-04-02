package com.mvp.vueseum.repository;

import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByExternalIdAndMuseum(String externalId, Museum museum);

    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.artist " +
            "LEFT JOIN FETCH a.museum " +
            "WHERE a.deleted = false")
    List<Artwork> findAllWithArtistsAndMuseums();

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.museum.id = :museumId")
    long countByMuseum(Long museumId);

    @Query("SELECT a.classification as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.classification IS NOT NULL " +
            "AND a.deleted = false " +
            "GROUP BY a.classification " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findClassificationsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.medium as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.medium IS NOT NULL " +
            "AND a.deleted = false " +
            "GROUP BY a.medium " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findMediumsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.country as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.country IS NOT NULL " +
            "AND a.deleted = false " +
            "GROUP BY a.country " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findGeographicLocationsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.region as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.region IS NOT NULL " +
            "AND a.deleted = false " +
            "GROUP BY a.region " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findRegionsWithCountsLimited(@Param("limit") int limit);

    @Query("SELECT a.culture as name, COUNT(a) as count FROM Artwork a " +
            "WHERE a.culture IS NOT NULL " +
            "AND a.deleted = false " +
            "GROUP BY a.culture " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> findCulturesWithCountsLimited(@Param("limit") int limit);

    @Query(nativeQuery = true, value =
            "SELECT a.* FROM artworks a " +
                    "WHERE a.is_deleted = false " +
                    "AND (:hasImage = false OR (a.image_url IS NOT NULL AND LENGTH(a.image_url) > 0)) " +
                    "AND (:title IS NULL OR a.title ILIKE CONCAT('%', :title, '%')) " +
                    "AND (:origin IS NULL OR " +
                    "     (a.culture IS NOT NULL AND a.culture ILIKE CONCAT('%', :origin, '%')) OR " +
                    "     (a.country IS NOT NULL AND a.country ILIKE CONCAT('%', :origin, '%'))) " +
                    "AND (:category IS NULL OR " +
                    "     (a.classification IS NOT NULL AND a.classification ILIKE CONCAT('%', :category, '%')) OR " +
                    "     (a.medium IS NOT NULL AND a.medium ILIKE CONCAT('%', :category, '%')))")
    Page<Artwork> findWithDateSort(
            @Param("hasImage") boolean hasImage,
            @Param("title") String title,
            @Param("origin") String origin,
            @Param("category") String category,
            Pageable pageable);

    List<Artwork> findByTitleContainingAndMuseumIdAndDeletedFalse(String titleFragment, Long museumId);

    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);

    @Modifying
    @Query("UPDATE Artwork a SET a.deleted = true, a.deletedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    void softDelete(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Artwork a SET a.deleted = true, a.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE a.externalId NOT IN :displayedIds AND a.museum.id = :museumId AND a.deleted = false")
    int softDeleteNonDisplayedArtworks(@Param("displayedIds") Set<String> displayedIds, @Param("museumId") Long museumId);

    @Query("SELECT a.id FROM Artwork a WHERE a.deleted = true AND a.deletedAt < :olderThan " +
            "AND NOT EXISTS (SELECT 1 FROM TourStop ts WHERE ts.artwork.id = a.id)")
    List<Long> findSoftDeletedArtworksNotInTours(@Param("olderThan") LocalDateTime olderThan);

    @Modifying
    @Query("DELETE FROM Artwork a WHERE a.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);
}