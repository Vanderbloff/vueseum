package com.mvp.artplatform.repository;

import com.mvp.artplatform.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    Optional<Tour> findByIdAndDeletedFalse(Long id);
}
