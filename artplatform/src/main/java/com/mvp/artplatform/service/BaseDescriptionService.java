package com.mvp.artplatform.service;

import com.mvp.artplatform.entity.Artwork;
import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.entity.TourStop;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseDescriptionService implements DescriptionGenerationService {

    /*
    We need three descriptions: one for the tour, one for the artwork, and one for the artwork specifically in the context of the tour
     */

    protected String buildStandardArtworkPrompt(Artwork artwork) {
        return String.format("""
            Create an engaging description for the artwork: %s %s (%s).
            
            Consider:
            - The artwork's historical context
            - %s
            - The artwork's significance in art history
            - Interesting details visitors should notice
            - How this piece fits into the broader museum experience
            
            Make the description engaging and informative for museum visitors,
            combining historical facts with interesting observations.
            Aim for a tone that is knowledgeable but accessible.
            """,
                artwork.getTitle(),
                artwork.hasKnownArtist() ? "by " + artwork.getArtistName() : "",
                artwork.getCreationDate(),
                artwork.hasKnownArtist() ? "The artist's technique and style" : "The artistic techniques used"
        );
    }

    protected String buildTourPrompt(List<Artwork> artworks, Tour.TourTheme theme) {
        return String.format("""
            Create an engaging museum tour description focused on the theme: %s.
            The tour includes the following artworks:
            %s
            
            Please create a cohesive narrative that:
            1. Introduces the theme and its significance
            2. Explains how each artwork contributes to the theme
            3. Highlights connections between the pieces
            4. Concludes with the theme's broader cultural impact
            
            Keep the tone engaging and accessible to general audiences while maintaining art historical accuracy.
            """,
                theme.name(),
                artworks.stream()
                        .map(art -> String.format("- %s by %s (%s)",
                                art.getTitle(),
                                art.getArtist().getArtistName(),
                                art.getCreationDate()))
                        .collect(Collectors.joining("\n"))
        );
    }

    protected String buildTourStopPrompt(TourStop stop) {
        return String.format("""
            Describe how %s by %s fits into this %s-themed tour.
            Consider:
            - How it exemplifies the tour's theme
            - Connections to other pieces in the tour
            - Its role in the broader narrative
            Focus on engaging storytelling that builds on the tour's theme.
            """,
                stop.getArtwork().getTitle(),
                stop.getArtwork().getArtist().getArtistName(),
                stop.getTour().getTheme()
        );
    }

    @Override
    public String generateArtworkDescription(Artwork artwork) {
        String prompt = buildStandardArtworkPrompt(artwork);
        return generateDescription(prompt);
    }

    @Override
    public String generateTourDescription(List<Artwork> artworks, Tour.TourTheme theme) {
        String prompt = buildTourPrompt(artworks, theme);
        return generateDescription(prompt);
    }

    @Override
    public String generateStopDescription(TourStop stop) {
        String prompt = buildTourStopPrompt(stop);
        return generateDescription(prompt);
    }

    protected abstract String generateDescription(String prompt);
}
