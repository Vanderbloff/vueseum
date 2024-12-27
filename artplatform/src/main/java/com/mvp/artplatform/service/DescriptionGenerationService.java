package com.mvp.artplatform.service;

import com.mvp.artplatform.entity.Artwork;
import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.entity.TourStop;

import java.util.List;

public interface DescriptionGenerationService {
    String generateArtworkDescription(Artwork artwork);
    String generateTourDescription(List<Artwork> artworks, Tour.TourTheme theme);
    String generateStopDescription(TourStop stop);
}
