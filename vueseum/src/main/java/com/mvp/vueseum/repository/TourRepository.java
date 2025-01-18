package com.mvp.vueseum.repository;

import com.mvp.vueseum.entity.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    Optional<Tour> findByIdAndDeletedFalse(Long id);
    long countByDeviceFingerprintAndDeletedFalse(String deviceFingerprint);
    Page<Tour> findByDeletedFalse(Pageable pageable);
}
