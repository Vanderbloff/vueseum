package com.mvp.artplatform.repository;

import com.mvp.artplatform.entity.Museum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MuseumRepository extends JpaRepository<Museum, Long> {
    Optional<Museum> findByName(String name);
}
