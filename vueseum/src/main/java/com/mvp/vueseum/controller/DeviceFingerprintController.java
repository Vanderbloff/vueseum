package com.mvp.vueseum.controller;

import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device")
@Slf4j
public class DeviceFingerprintController {
    private final DeviceFingerprintService fingerprintService;

    @GetMapping("/fingerprint")
    public ResponseEntity<Map<String, String>> getFingerprint(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.debug("Fingerprint requested from IP: {}", request.getRemoteAddr());

        String fingerprint = fingerprintService.getOrCreateFingerprint(request, response);
        log.debug("Returning fingerprint: {}", fingerprint);

        return ResponseEntity.ok(Map.of("fingerprint", fingerprint));
    }
}