package com.mvp.vueseum.service.visitor;

import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.entity.DeviceFingerprint;
import com.mvp.vueseum.repository.DeviceFingerprintRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintService {
    private final Cache<String, String> deviceFingerprintCache;
    private final DeviceFingerprintRepository fingerprintRepository;
    private static final String FINGERPRINT_TOKEN_COOKIE = "VUESEUM_DEVICE";

    /**
     * Creates or retrieves a fingerprint for the device.
     * Stores a persistent token in a cookie to maintain identity.
     */
    @Transactional
    public String getOrCreateFingerprint(HttpServletRequest request, HttpServletResponse response) {
        // Check if we have a token already
        String token = getTokenFromRequest(request);
        String cachedFingerprint = null;

        if (token != null) {
            cachedFingerprint = deviceFingerprintCache.getIfPresent(token);
            log.debug("Found token in request: {}", token);

            if (cachedFingerprint == null) {
                String finalToken = token;
                cachedFingerprint = fingerprintRepository.findByToken(token)
                        .map(entity -> {
                            fingerprintRepository.updateLastAccessedAt(finalToken, LocalDateTime.now());
                            String fingerprint = entity.getFingerprint();
                            deviceFingerprintCache.put(finalToken, fingerprint);
                            return fingerprint;
                        })
                        .orElse(null);
            }

            log.debug("Retrieved fingerprint: {}", cachedFingerprint);
        }

        // If we don't have a token, or it's not in our cache/database, create new one
        if (token == null || cachedFingerprint == null) {
            token = UUID.randomUUID().toString();
            String fingerprint = generateFingerprint(request);

            deviceFingerprintCache.put(token, fingerprint);
            saveFingerprint(request, token, fingerprint);

            log.debug("Generated new fingerprint: {}", fingerprint);
            log.debug("Saved with token: {}", token);

            // Set cookie for future requests
            setCookieInResponse(response, token);
            return fingerprint;
        }

        return cachedFingerprint;
    }

    /**
     * Gets the stored fingerprint using the token from the request.
     * Returns null if no valid fingerprint is found.
     */
    public String getStoredFingerprint(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            log.debug("No device token found in request");
            return null;
        }

        String fingerprint = deviceFingerprintCache.getIfPresent(token);

        if (fingerprint == null) {
            fingerprint = fingerprintRepository.findByToken(token)
                    .map(entity -> {
                        // Update cache with value from database
                        String fp = entity.getFingerprint();
                        deviceFingerprintCache.put(token, fp);
                        return fp;
                    })
                    .orElse(null);
        }

        log.debug("Retrieved stored fingerprint for token {}: {}", token, fingerprint);
        return fingerprint;
    }

    /**
     * Saves fingerprint information to database
     */
    @Transactional
    private void saveFingerprint(HttpServletRequest request, String token, String fingerprint) {
        DeviceFingerprint entity = DeviceFingerprint.builder()
                .token(token)
                .fingerprint(fingerprint)
                .userAgent(request.getHeader("User-Agent"))
                .screenResolution(request.getHeader("X-Screen-Resolution"))
                .timezone(request.getHeader("X-Timezone"))
                .languages(request.getHeader("Accept-Language"))
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        fingerprintRepository.save(entity);
    }

    /**
     * Creates a unique fingerprint from device characteristics.
     */
    public String generateFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        // Required header
        String userAgent = request.getHeader("User-Agent");
        fingerprint.append(userAgent != null ? userAgent : "unknown");
        fingerprint.append("|");

        // Optional headers with defaults
        String screenResolution = request.getHeader("X-Screen-Resolution");
        fingerprint.append(screenResolution != null ? screenResolution : "default");
        fingerprint.append("|");

        String timezone = request.getHeader("X-Timezone");
        fingerprint.append(timezone != null ? timezone : "UTC");
        fingerprint.append("|");

        String languages = request.getHeader("Accept-Language");
        fingerprint.append(languages != null ? languages : "en");

        return DigestUtils.sha256Hex(fingerprint.toString());
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (FINGERPRINT_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setCookieInResponse(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(FINGERPRINT_TOKEN_COOKIE, token);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        cookie.setHttpOnly(true); // Not accessible via JavaScript
        cookie.setSecure(true);   // Only sent over HTTPS
        response.addCookie(cookie);

        log.debug("Set fingerprint token cookie: {}", token);
    }
}