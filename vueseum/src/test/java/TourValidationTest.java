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

            tour.addStop(art2, 1);
            tour.addStop(art3, 2);
            tour.addStop(art1, 0);

            List<TourStop> orderedStops = new ArrayList<>(tour.getStops());
            assertThat(orderedStops)
                    .extracting(TourStop::getSequenceNumber)
                    .containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("should reject invalid sequence numbers")
        void invalidSequenceNumber() {
            Artwork artwork = new Artwork();
            assertThatThrownBy(() -> tour.addStop(artwork, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sequence number must be non-negative");
        }

        @Test
        @DisplayName("should maintain order after multiple additions")
        void maintainOrderAfterMultipleAdditions() {
            Artwork art1 = new Artwork();
            Artwork art2 = new Artwork();
            Artwork art3 = new Artwork();

            tour.addStop(art3, 2);
            tour.addStop(art2, 1);
            tour.addStop(art1, 0);

            Artwork art4 = new Artwork();
            tour.addStop(art4, 1);

            List<TourStop> orderedStops = tour.getOrderedStops();
            assertThat(orderedStops)
                    .extracting(TourStop::getSequenceNumber)
                    .containsExactly(0, 1, 2, 3);
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

    @Nested
    @DisplayName("Description Generation Tests")
    class DescriptionGenerationTests {
        @Test
        @DisplayName("should handle AI generation failure gracefully")
        void aiGenerationFailure() {
            DescriptionGenerationService failingService = mock(DescriptionGenerationService.class);
            when(failingService.generateStopDescription(any()))
                    .thenThrow(new AiProviderException("API error"));

            TourStop stop = new TourStop();
            stop.setArtwork(new Artwork());
            stop.setSequenceNumber(0);
            tour.getStops().add(stop);

            tour.generateDescriptionsForAllStops(failingService);

            assertThat(stop.getTourContextDescription())
                    .isEqualTo("Description temporarily unavailable");
        }
    }
}