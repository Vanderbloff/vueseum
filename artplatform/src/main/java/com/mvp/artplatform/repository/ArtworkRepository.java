package com.mvp.artplatform.repository;

import com.mvp.artplatform.model.Artwork;
import com.mvp.artplatform.model.Museum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    Optional<Artwork> findByExternalIdAndMuseum(String externalId, Museum museum);
}
