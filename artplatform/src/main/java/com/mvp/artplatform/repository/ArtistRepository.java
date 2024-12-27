package com.mvp.artplatform.repository;

import com.mvp.artplatform.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByArtistName(String artistName);

    /**
     * Finds artists whose names start with the given prefix,
     * but only if they have artworks currently on display.
     * We limit results to avoid overwhelming the user.
     */
    @Query("""
        SELECT DISTINCT a FROM Artist a
        JOIN a.works w
        WHERE LOWER(a.artistName) LIKE LOWER(CONCAT(:prefix, '%'))
        AND w.isOnDisplay = true
        AND w.museum.id = :museumId
        ORDER BY a.artistName
        LIMIT 10
        """)
    List<Artist> findSuggestedArtists(
            @Param("prefix") String prefix,
            @Param("museumId") Long museumId
    );
}
