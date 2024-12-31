package com.mvp.vueseum.service.visitor;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
public class DeviceFingerprintService {
    /**
     * Creates a unique fingerprint from device characteristics.
     * This combines multiple factors to create a reasonably unique identifier
     * while respecting user privacy.
     */
    public String generateFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        // Required header
        String userAgent = request.getHeader("User-Agent");
        fingerprint.append(userAgent != null ? userAgent : "unknown");
        fingerprint.append("|"); // Add separator to prevent collisions

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
}
