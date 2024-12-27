package com.mvp.artplatform.service.visitor;

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

        // Add user agent
        String userAgent = request.getHeader("User-Agent");
        fingerprint.append(userAgent);

        // Add screen resolution (from client-side)
        String screenResolution = request.getHeader("X-Screen-Resolution");
        fingerprint.append(screenResolution);

        // Add timezone
        String timezone = request.getHeader("X-Timezone");
        fingerprint.append(timezone);

        // Add accepted languages
        String languages = request.getHeader("Accept-Language");
        fingerprint.append(languages);

        // Generate a hash of all these characteristics
        return DigestUtils.sha256Hex(fingerprint.toString());
    }
}
