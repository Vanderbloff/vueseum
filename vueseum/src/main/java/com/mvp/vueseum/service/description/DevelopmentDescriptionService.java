package com.mvp.vueseum.service.description;

import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;
import com.mvp.vueseum.service.DescriptionGenerationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Primary
@ConditionalOnProperty(name = "ai.provider", havingValue = "none")
public class DevelopmentDescriptionService implements DescriptionGenerationService {

    @Override
    public String generateArtworkDescription(Artwork artwork) {
        return String.format(
                "Development description for %s by %s. Located in gallery %s.",
                artwork.getTitle(),
                artwork.getArtist() != null ? artwork.getArtist().getArtistName() : "Unknown Artist",
                artwork.getGalleryNumber()
        );
    }

    @Override
    public String generateTourDescription(List<Artwork> artworks, Tour.TourTheme theme) {
        String artworkList = artworks.stream()
                .map(Artwork::getTitle)
                .collect(Collectors.joining(", "));

        return String.format(
                "Development tour with theme %s featuring: %s",
                theme,
                artworkList
        );
    }

    @Override
    public String generateStopDescription(TourStop stop) {
        return String.format(
                "Development description for stop #%d featuring %s",
                stop.getSequenceNumber(),
                stop.getArtwork().getTitle()
        );
    }
}