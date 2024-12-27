package com.mvp.artplatform.controller;

import com.mvp.artplatform.domain.TourGenerationProgress;
import com.mvp.artplatform.domain.TourGenerationRequest;
import com.mvp.artplatform.domain.TourUpdateRequest;
import com.mvp.artplatform.dto.TourDTO;
import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.event.TourProgressListener;
import com.mvp.artplatform.service.tour.TourService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;
    private final TourProgressListener progressListener;

    /**
     * Generates a new tour based on user preferences and museum context.
     * Handles progress tracking and error cases.
     */
    @PostMapping("/generate")
    public ResponseEntity<TourDTO> generateTour(
            @Valid @RequestBody TourGenerationRequest request,
            HttpServletRequest httpRequest) {
        Tour tour = tourService.generateTour(request, httpRequest);
        return ResponseEntity.ok(TourDTO.fromEntity(tour));
    }

    /**
     * Retrieves an existing tour by ID.
     * Returns 404 if tour doesn't exist or is not accessible to the user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TourDTO> getTour(@PathVariable Long id) {
        return tourService.findTourById(id)
                .map(TourDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing tour's metadata (name, description, etc.).
     * Does not modify tour stops or structure.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TourDTO> updateTour(
            @PathVariable Long id,
            @Valid @RequestBody TourUpdateRequest request) {
        return tourService.updateTour(id, request)
                .map(TourDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the current progress/status of a tour generation request.
     * Useful for long-running tour generations.
     */
    @GetMapping("/generation/{requestId}/status")
    public ResponseEntity<TourGenerationProgress> getGenerationStatus(
            @PathVariable String requestId) {
        return progressListener.getProgress(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancels an in-progress tour generation.
     */
    @DeleteMapping("/generation/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelGeneration(@PathVariable String requestId) {
        tourService.cancelGeneration(requestId);
    }

    /**
     * Deletes an existing tour.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
    }
}
