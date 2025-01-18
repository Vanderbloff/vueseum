import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;
import com.mvp.vueseum.exception.AiProviderException;
import com.mvp.vueseum.service.DescriptionGenerationService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourValidationTest {
    private Tour tour;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        factory.getValidator();
        tour = new Tour();
        tour.setMuseum(new Museum());
        tour.setDeviceFingerprint("test-device");
        tour.setName("Test Tour");
    }

    @Nested
    @DisplayName("Stop Management Tests")
    class StopManagementTests {
        @Test
        @DisplayName("should maintain stop sequence order")
        void stopSequenceOrder() {
            Artwork art1 = new Artwork();
            Artwork art2 = new Artwork();
            Artwork art3 = new Artwork();

            // Add stops in random order
            tour.addStop(art2, 1);
            tour.addStop(art3, 2);
            tour.addStop(art1, 0);

            // Verify TreeSet maintains order
            List<TourStop> stops = tour.getStops();
            assertThat(stops)
                    .extracting(TourStop::getSequenceNumber)
                    .containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("should handle duplicate sequence numbers")
        void duplicateSequenceNumbers() {
            Artwork art1 = new Artwork();
            art1.setTitle("First Artwork");
            Artwork art2 = new Artwork();
            art2.setTitle("Second Artwork");

            tour.addStop(art1, 1);
            tour.addStop(art2, 1);

            List<TourStop> stops = tour.getStops();
            assertThat(stops).hasSize(1);
            assertThat(stops.getFirst().getArtwork().getTitle())
                    .isEqualTo("First Artwork");
        }

        @Test
        @DisplayName("should reject invalid sequence numbers")
        void invalidSequenceNumber() {
            Artwork artwork = new Artwork();
            assertThatThrownBy(() -> tour.addStop(artwork, -1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Sequence number must be non-negative");
        }
    }

    @Nested
    @DisplayName("Theme Tests")
    class ThemeTests {
        @Test
        @DisplayName("should handle theme changes")
        void themeChange() {
            tour.setTheme(Tour.TourTheme.CHRONOLOGICAL);
            assertThat(tour.getTheme().title)
                    .isEqualTo("Art through the ages");

            tour.setTheme(Tour.TourTheme.CULTURAL);
            assertThat(tour.getTheme().description)
                    .isEqualTo("Explore art across cultures");
        }
    }

    @Test
    @DisplayName("should handle AI generation failure gracefully")
    void aiGenerationFailure() {
        DescriptionGenerationService failingService = mock(DescriptionGenerationService.class);
        when(failingService.generateStopDescription(any()))
                .thenThrow(new AiProviderException("API error"));

        // Use proper constructor instead of no-args
        Artwork artwork = new Artwork();
        tour.addStop(artwork, 0);  // This will create the TourStop properly

        tour.generateDescriptionsForAllStops(failingService);

        // Get the first (and only) stop to verify
        TourStop stop = tour.getStops().getFirst();
        assertThat(stop.getTourContextDescription())
                .isEqualTo("Description temporarily unavailable");
    }
}