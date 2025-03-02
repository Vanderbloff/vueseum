package com.mvp.vueseum.service;

import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseDescriptionService implements DescriptionGenerationService {

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
                artwork.getFullAttribution(),
                artwork.getCreationDate(),
                artwork.hasKnownArtist() ? "The artist's technique and style" : "The artistic techniques used"
        );
    }

    protected String buildTourPrompt(List<Artwork> artworks, Tour.TourTheme theme) {
        return String.format("""
        Create an engaging museum tour description with a creative title focused on the theme: %s.
        
        FORMAT YOUR RESPONSE EXACTLY LIKE THIS:
        TITLE: [Your creative, specific title that reflects the content of the tour, without any quotation marks or special formatting]
        
        [Your tour description here]
        
        The tour includes the following artworks:
        %s
        
        For the TITLE:
        - Create a specific, engaging title that captures the essence of these artworks
        - Highlight key artists, periods, or cultures represented when appropriate
        - Reflect the %s theme in your title
        
        For the DESCRIPTION:
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
                                art.getArtistName(),
                                art.getCreationDate()))
                        .collect(Collectors.joining("\n")),
                theme.name()
        );
    }

    /**
     * Builds a prompt for generating a description for a tour stop that considers
     * the stop's position in the tour and relationships to other stops.
     *
     * @param stop The tour stop to describe
     * @param allStops All stops in the tour, for context
     * @return A prompt for generating the stop description
     */
    protected String buildTourStopPrompt(TourStop stop, List<TourStop> allStops) {
        StringBuilder prompt = new StringBuilder();

        // Basic information
        prompt.append(String.format("""
        Create an informational description about "%s" by %s for a museum tour.
        
        IMPORTANT FORMATTING INSTRUCTIONS:
        - Write in plain text only - DO NOT use markdown formatting, bold, or italic markers
        - Use simple paragraph breaks for structure
        - Keep sentences clear and concise
        - DO NOT use bullet points or numbered lists
        
        IMPORTANT STYLE GUIDELINES:
        - Use third-person perspective throughout
        - AVOID phrases like "we see," "as you can see," "thank you for joining," etc.
        - AVOID addressing the viewer directly with "you" or "your"
        - Write as an informational document, not as a tour guide speaking
        - Focus on art historical information and context
        - Maintain an engaging, educational tone without directly addressing the reader
        
        """,
                stop.getArtwork().getTitle(),
                stop.getArtwork().getFullAttribution()));

        // Add artwork details for context
        prompt.append(String.format("""
        ARTWORK DETAILS:
        - Title: %s
        - Artist: %s
        - Date: %s
        - Medium: %s
        - Culture/Origin: %s
        
        """,
                stop.getArtwork().getTitle(),
                stop.getArtwork().getFullAttribution(),
                stop.getArtwork().getCreationDate(),
                stop.getArtwork().getMedium(),
                stop.getArtwork().getCulture() != null ? stop.getArtwork().getCulture() : "Unknown"));

        // Add tour context
        prompt.append(String.format("""
        TOUR CONTEXT:
        - Tour Theme: %s
        - Total Stops: %d
        - This is stop #%d in the sequence
        """,
                stop.getTour().getTheme().name(),
                allStops.size(),
                stop.getSequenceNumber()));

        // Add information about previous stop if available
        if (stop.getSequenceNumber() > 1) {
            allStops.stream()
                    .filter(s -> s.getSequenceNumber() == stop.getSequenceNumber() - 1)
                    .findFirst().ifPresent(prevStop -> prompt.append(String.format("""
                                    
                                    PREVIOUS STOP:
                                    - Title: %s
                                    - Artist: %s
                                    - Date: %s
                                    - Connection: Explain how this artwork connects to the previous piece
                                    """,
                            prevStop.getArtwork().getTitle(),
                            prevStop.getArtwork().getArtistName(),
                            prevStop.getArtwork().getCreationDate())));

        }

        // Add information about next stop if available
        if (stop.getSequenceNumber() < allStops.size()) {
            allStops.stream()
                    .filter(s -> s.getSequenceNumber() == stop.getSequenceNumber() + 1)
                    .findFirst().ifPresent(nextStop -> prompt.append(String.format("""
                                    
                                    NEXT STOP:
                                    - Title: %s
                                    - Artist: %s
                                    - Date: %s
                                    - Transition: Create a smooth transition to this upcoming piece
                                    """,
                            nextStop.getArtwork().getTitle(),
                            nextStop.getArtwork().getArtistName(),
                            nextStop.getArtwork().getCreationDate())));

        }

        // Narrative position-specific instructions
        if (stop.getSequenceNumber() == 1) {
            prompt.append("""
            
            SPECIAL INSTRUCTIONS (FIRST STOP):
            - Introduce the tour theme and what visitors will experience
            - Begin the narrative that will connect all stops
            - Set the tone for the entire tour experience
            """);
        } else if (stop.getSequenceNumber() == allStops.size()) {
            prompt.append("""
            
            SPECIAL INSTRUCTIONS (FINAL STOP):
            - Bring closure to the tour's narrative
            - Summarize how this piece completes the thematic journey
            - Leave visitors with a memorable final insight
            """);
        } else {
            prompt.append("""
            
            SPECIAL INSTRUCTIONS (MIDDLE STOP):
            - Develop the tour's narrative further
            - Create meaningful connections to both previous and upcoming pieces
            - Highlight how this artwork advances the tour's theme
            """);
        }

        // Final instructions
        prompt.append("""
        
        KEY OBJECTIVES FOR THIS DESCRIPTION:
        1. Make the artwork accessible and interesting to general visitors
        2. Provide meaningful connections to the tour's theme
        3. Create narrative continuity with other stops
        4. Offer specific observations about visual elements visitors should notice
        5. Keep the description under 250 words
        
        Remember to avoid markdown formatting and maintain a conversational style throughout.
        """);

        return prompt.toString();
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
        List<TourStop> allStops = stop.getTour().getStops()
                .stream()
                .sorted(Comparator.comparingInt(TourStop::getSequenceNumber))
                .collect(Collectors.toList());

        String prompt = buildTourStopPrompt(stop, allStops);
        String description = generateDescription(prompt);
        return cleanupFormatting(description);
    }

    /**
     * Cleans up a generated description by removing any markdown formatting
     * that might have been included despite instructions.
     */
    protected String cleanupFormatting(String description) {
        if (description == null) {
            return null;
        }

        // Remove markdown bold/italic markers
        String cleaned = description.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("\\*(.+?)\\*", "$1");

        // Remove markdown headers
        cleaned = cleaned.replaceAll("#+\\s+", "");

        // Remove markdown list markers but keep the content
        cleaned = cleaned.replaceAll("^\\s*-\\s+", "• ");
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\.\\s+", "• ");

        // Normalize multiple newlines to maximum of two
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned;
    }

    protected abstract String generateDescription(String prompt);
}
