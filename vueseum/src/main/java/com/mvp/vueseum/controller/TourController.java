package com.mvp.vueseum.controller;

import com.mvp.vueseum.domain.TourGenerationProgress;
import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.dto.TourDTO;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.service.tour.TourService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;
    private final TourProgressListener progressListener;

    @GetMapping
    public ResponseEntity<Page<TourDTO>> getTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Tour> tours = tourService.getTourPage(PageRequest.of(page, size));
        return ResponseEntity.ok(tours.map(TourDTO::fromEntity));
    }

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
    public ResponseEntity<TourDTO> updateTourDetails(
            @PathVariable Long id,
            @Valid @RequestBody TourUpdateRequest request) {
        return tourService.updateTourDetails(id, request)
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
     * Deletes an existing tour.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTour(@PathVariable Long id) {
        tourService.deleteTour(id);
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateTour(@PathVariable Long id) {
        return ResponseEntity.ok(tourService.validateTour(id));
    }
}
