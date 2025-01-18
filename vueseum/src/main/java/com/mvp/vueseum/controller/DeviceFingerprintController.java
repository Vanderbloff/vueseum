package com.mvp.vueseum.controller;

import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device")
public class DeviceFingerprintController {
    private final DeviceFingerprintService fingerprintService;

    @GetMapping("/fingerprint")
    public ResponseEntity<String> getFingerprint(HttpServletRequest request) {
        return ResponseEntity.ok(fingerprintService.generateFingerprint(request));
    }
}
