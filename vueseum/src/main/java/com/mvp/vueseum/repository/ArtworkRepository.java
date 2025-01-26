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
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    @Query("SELECT DISTINCT a.classification FROM Artwork a WHERE a.classification IS NOT NULL")
    List<String> findDistinctClassifications();

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL")
    List<String> findDistinctMediums();

    @Query("SELECT DISTINCT a.culture FROM Artwork a WHERE a.culture IS NOT NULL")
    List<String> findDistinctCultures();

    Optional<Artwork> findByExternalIdAndMuseum(String externalId, Museum museum);
    @NotNull Page<Artwork> findAll(Specification<Artwork> specification, @NotNull Pageable pageable);

    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN FETCH a.artist " +
            "LEFT JOIN FETCH a.museum")
    List<Artwork> findAllWithArtistsAndMuseums();

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.museum.id = :museumId")
    long countByMuseum(Long museumId);
}
