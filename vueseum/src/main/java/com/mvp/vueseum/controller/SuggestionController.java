package com.mvp.vueseum.controller;

import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.service.tour.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<SuggestionService.Suggestion>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam String type,
            @RequestParam String museumId,
            @ModelAttribute TourPreferences preferences) {

        SuggestionService.SuggestionType suggestionType;
        try {
            suggestionType = SuggestionService.SuggestionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        long museumIdLong;
        try {
            museumIdLong = Long.parseLong(museumId);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        TourPreferences prefs = preferences != null ? preferences : new TourPreferences();
        return ResponseEntity.ok(suggestionService.getSuggestions(prefix, suggestionType, museumIdLong, prefs));
    }
}
