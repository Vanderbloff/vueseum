package com.mvp.vueseum.controller;

import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.service.tour.FilterOptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/filters")
@RequiredArgsConstructor
public class FilterController {
    private final FilterOptionsService filterOptionsService;

    @PostMapping("/options")
    public ResponseEntity<FilterOptionsService.FilterOptions> getFilterOptions(
            @RequestParam Long museumId,
            @RequestBody TourPreferences preferences) {
        return ResponseEntity.ok(filterOptionsService.getAvailableOptions(preferences, museumId));
    }
}
