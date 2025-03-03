package com.mvp.vueseum.controller;

import com.mvp.vueseum.domain.TourGenerationProgress;
import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.dto.TourDTO;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.service.tour.TourService;
import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Slf4j
public class TourController {
    private final TourService tourService;
    private final TourProgressListener progressListener;
    private final DeviceFingerprintService deviceFingerprintService;

    /**
     * Returns only tours that belong to the current device fingerprint
     */
    @GetMapping
    public ResponseEntity<Page<TourDTO>> getTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(request);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for tour list request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Page<Tour> tours = tourService.getTourPageForDevice(
                deviceFingerprint,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(tours.map(TourDTO::fromEntity));
    }

    /**
     * Generates a new tour based on user preferences and museum context.
     * Uses the device fingerprint from the request.
     */
    @PostMapping("/generate")
    public ResponseEntity<TourDTO> generateTour(
            @Valid @RequestBody TourGenerationRequest request,
            HttpServletRequest httpRequest) {
        Tour tour = tourService.generateTour(request, httpRequest);
        return ResponseEntity.ok(TourDTO.fromEntity(tour));
    }

    /**
     * Retrieves an existing tour by ID but only if it belongs to the current device.
     * Returns 404 if tour doesn't exist or is not accessible to the user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TourDTO> getTour(
            @PathVariable Long id,
            HttpServletRequest request) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(request);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for tour detail request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return tourService.findTourByIdForDevice(id, deviceFingerprint)
                .map(TourDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing tour's metadata, but only if it belongs to the current device.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TourDTO> updateTourDetails(
            @PathVariable Long id,
            @Valid @RequestBody TourUpdateRequest request,
            HttpServletRequest httpRequest) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(httpRequest);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for tour update request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return tourService.updateTourDetailsForDevice(id, request, deviceFingerprint)
                .map(TourDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the current progress/status of a tour generation request.
     */
    @GetMapping("/generation/{requestId}/status")
    public ResponseEntity<TourGenerationProgress> getGenerationStatus(
            @PathVariable String requestId,
            HttpServletRequest request) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(request);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for progress status request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return progressListener.getProgressForDevice(requestId, deviceFingerprint)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a tour, but only if it belongs to the current device.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTour(
            @PathVariable Long id,
            HttpServletRequest request) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(request);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for tour delete request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean deleted = tourService.deleteTourForDevice(id, deviceFingerprint);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Validates a tour, but only if it belongs to the current device.
     */
    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateTour(
            @PathVariable Long id,
            HttpServletRequest request) {

        String deviceFingerprint = deviceFingerprintService.getStoredFingerprint(request);
        if (deviceFingerprint == null) {
            log.warn("No device fingerprint found for tour validation request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> validationResult = tourService.validateTourForDevice(id, deviceFingerprint);
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}