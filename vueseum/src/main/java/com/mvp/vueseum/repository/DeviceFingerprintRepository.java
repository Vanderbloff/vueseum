package com.mvp.vueseum.repository;

import com.mvp.vueseum.entity.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, String> {

    Optional<DeviceFingerprint> findByToken(String token);

    @Modifying
    @Query("UPDATE DeviceFingerprint d SET d.lastAccessedAt = :timestamp WHERE d.token = :token")
    void updateLastAccessedAt(String token, LocalDateTime timestamp);
}