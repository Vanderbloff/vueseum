import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ArtworkValidationTest {
    private Artwork artwork;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        factory.getValidator();
        artwork = new Artwork();
    }

    @Nested
    @DisplayName("Attribution Tests")
    class AttributionTests {
        @Test
        @DisplayName("should identify confident attribution correctly")
        void confidentAttribution() {
            Artist artist = new Artist();
            artist.setArtistName("Test Artist");
            artwork.setArtist(artist);

            assertThat(artwork.isConfidentAttribution()).isTrue();

            artwork.setArtistPrefix("Attributed to");
            assertThat(artwork.isConfidentAttribution()).isFalse();
        }

        @Test
        @DisplayName("should handle null artist gracefully")
        void nullArtist() {
            artwork.setArtist(null);
            assertThat(artwork.getArtistName()).isEqualTo("Unknown Artist");
            assertThat(artwork.isConfidentAttribution()).isFalse();
        }
    }

    @Nested
    @DisplayName("Museum Relationship")
    class MuseumRelationship {
        @Test
        @DisplayName("should maintain bidirectional relationship")
        void bidirectionalRelationship() {
            Museum museum = new Museum();
            artwork.setMuseum(museum);

            assertThat(museum.getCollection()).contains(artwork);
            assertThat(artwork.getMuseum()).isSameAs(museum);
        }
    }

    @Nested
    @DisplayName("Processing Status Tests")
    class ProcessingStatusTests {
        @Test
        @DisplayName("should track processing errors correctly")
        void processingErrorTracking() {
            artwork.setProcessingStatus(Artwork.ProcessingStatus.ERROR);
            artwork.setLastSyncError("API timeout");
            artwork.setLastSyncAttempt(LocalDateTime.now());

            assertThat(artwork.getProcessingStatus()).isEqualTo(Artwork.ProcessingStatus.ERROR);
            assertThat(artwork.getLastSyncError()).isNotEmpty();
        }
    }
}