package com.mvp.vueseum.service;

import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;

import java.util.List;

public interface DescriptionGenerationService {
    String generateArtworkDescription(Artwork artwork);
    String generateTourDescription(List<Artwork> artworks, Tour.TourTheme theme);
    String generateStopDescription(TourStop stop);
}
