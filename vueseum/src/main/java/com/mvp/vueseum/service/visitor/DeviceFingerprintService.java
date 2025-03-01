package com.mvp.vueseum.service.visitor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintService {
    private final Cache<String, String> deviceFingerprintCache;
    private static final String FINGERPRINT_TOKEN_COOKIE = "VUESEUM_DEVICE";

    /**
     * Creates or retrieves a fingerprint for the device.
     * Stores a persistent token in a cookie to maintain identity.
     */
    public String getOrCreateFingerprint(HttpServletRequest request, HttpServletResponse response) {
        // Check if we have a token already
        String token = getTokenFromRequest(request);
        String cachedFingerprint = null;

        if (token != null) {
            cachedFingerprint = deviceFingerprintCache.getIfPresent(token);
            log.debug("Found token in request: {}", token);
            log.debug("Retrieved fingerprint from cache: {}", cachedFingerprint);
        }

        // If we don't have a token, or it's not in our cache, create new one
        if (token == null || cachedFingerprint == null) {
            token = UUID.randomUUID().toString();
            String fingerprint = generateFingerprint(request);
            deviceFingerprintCache.put(token, fingerprint);

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
        log.debug("Retrieved stored fingerprint for token {}: {}", token, fingerprint);
        return fingerprint;
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
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(FINGERPRINT_TOKEN_COOKIE, token);
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        cookie.setHttpOnly(true); // Not accessible via JavaScript
        cookie.setSecure(true);   // Only sent over HTTPS
        response.addCookie(cookie);

        log.debug("Set fingerprint token cookie: {}", token);
    }
}