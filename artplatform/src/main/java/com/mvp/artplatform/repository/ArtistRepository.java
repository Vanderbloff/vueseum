package com.mvp.artplatform.repository;

import com.mvp.artplatform.model.Artist;
import com.mvp.artplatform.model.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByArtistName(String artistName);
}
