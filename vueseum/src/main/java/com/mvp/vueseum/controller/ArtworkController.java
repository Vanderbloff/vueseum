package com.mvp.vueseum.controller;


import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.dto.ArtworkDetailsDTO;
import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.service.artwork.ArtworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/v1/artworks")
public class ArtworkController {

    private final ArtworkService artworkService;

    @Operation(summary = "Search artworks", description = "Search artworks across all connected museums with filtering")
    @GetMapping
    public ResponseEntity<Page<ArtworkDetailsDTO>> searchArtworks(
            @ModelAttribute @Valid ArtworkSearchCriteria criteria,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ArtworkDetails> artworkPage = artworkService.searchArtworks(criteria, pageable);
        Page<ArtworkDetailsDTO> dtoPage = artworkPage.map(ArtworkDetailsDTO::fromArtworkDetails);

        return ResponseEntity.ok(dtoPage);
    }

    @Operation(summary = "Get artwork by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ArtworkDetailsDTO> getArtwork(
            @Parameter(description = "Artwork ID from museum API")
            @PathVariable String id,
            @Parameter(description = "Museum ID", required = true)
            @RequestParam Long museumId) {
        ArtworkDetails details = artworkService.fetchArtworkById(id, museumId);
        return ResponseEntity.ok(ArtworkDetailsDTO.fromArtworkDetails(details));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions(
            @ModelAttribute ArtworkSearchCriteria criteria) {
        Map<String, List<String>> options = artworkService.getFilterOptions(criteria);
        return ResponseEntity.ok(options);
    }
}
