package com.mvp.vueseum.controller;

import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.service.tour.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) String preferredArtworks,
            @RequestParam(required = false) String preferredArtists,
            @RequestParam(required = false) String preferredMediums,
            @RequestParam(required = false) String preferredCultures,
            @RequestParam(required = false) String preferredPeriods) {

        try {
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

            TourPreferences preferences = new TourPreferences();
            preferences.setMuseumId(museumIdLong);

            if (preferredArtworks != null && !preferredArtworks.isEmpty()) {
                preferences.setRequiredArtworkIds(parseArtworkIds(preferredArtworks));
            }

            if (preferredArtists != null && !preferredArtists.isEmpty()) {
                preferences.setPreferredArtists(new HashSet<>(List.of(preferredArtists.split(","))));
            }

            if (preferredMediums != null && !preferredMediums.isEmpty()) {
                preferences.setPreferredMediums(new HashSet<>(List.of(preferredMediums.split(","))));
            }

            if (preferredCultures != null && !preferredCultures.isEmpty()) {
                preferences.setPreferredCultures(new HashSet<>(List.of(preferredCultures.split(","))));
            }

            if (preferredPeriods != null && !preferredPeriods.isEmpty()) {
                preferences.setPreferredPeriods(new HashSet<>(List.of(preferredPeriods.split(","))));
            }

            return ResponseEntity.ok(suggestionService.getSuggestions(prefix, suggestionType, museumIdLong, preferences));

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    private Set<Long> parseArtworkIds(String artworkIdsStr) {
        try {
            return Arrays.stream(artworkIdsStr.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            return new HashSet<>();
        }
    }
}