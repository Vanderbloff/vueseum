package com.mvp.artplatform.controller;

import com.mvp.artplatform.model.Artwork;
import com.mvp.artplatform.service.ArtworkAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "api/v1/artworks")
public class ArtworkController {

    private final ArtworkAggregationService artworkAggregationService;

    @Autowired
    public ArtworkController(ArtworkAggregationService artworkAggregationService) {
        this.artworkAggregationService = artworkAggregationService;
    }

    /*@GetMapping
    public List<Artwork> getAllArtworks() {
        return artworkAggregationService.getAllArtworks();
    }*/
}
