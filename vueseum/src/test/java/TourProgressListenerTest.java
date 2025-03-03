import com.mvp.vueseum.domain.TourGenerationProgress;
import com.mvp.vueseum.event.TourProgressListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class TourProgressListenerTest {
    private TourProgressListener progressListener;
    private static final String TEST_REQUEST_ID = "test-request-123";
    private static final String TEST_VISITOR_ID = "test-visitor-456";

    @BeforeEach
    void setUp() {
        progressListener = new TourProgressListener();
    }

    @Test
    void whenInitializingProgress_thenCreatesNewTracking() {
        progressListener.initializeProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);

        Optional<TourGenerationProgress> progress = progressListener.getProgressForDevice(TEST_REQUEST_ID, TEST_VISITOR_ID);
        assertThat(progress)
                .isPresent()
                .hasValueSatisfying(p -> {
                    assertThat(p.getRequestId()).isEqualTo(TEST_REQUEST_ID);
                    assertThat(p.getVisitorId()).isEqualTo(TEST_VISITOR_ID);
                    assertThat(p.getProgress()).isZero();
                    assertThat(p.getCurrentTask()).isEqualTo("Starting tour generation...");
                });
    }

    @Test
    void whenUpdatingProgress_thenUpdatesTrackingCorrectly() {
        progressListener.initializeProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);

        progressListener.updateProgress(TEST_REQUEST_ID, 0.5, "Selecting artworks");

        Optional<TourGenerationProgress> progress = progressListener.getProgressForDevice(TEST_REQUEST_ID, TEST_VISITOR_ID);
        assertThat(progress)
                .isPresent()
                .hasValueSatisfying(p -> {
                    assertThat(p.getProgress()).isEqualTo(0.5);
                    assertThat(p.getCurrentTask()).isEqualTo("Selecting artworks");
                });
    }

    @Test
    void whenProgressIsComplete_thenRemovesTracking() {
        progressListener.initializeProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);

        progressListener.updateProgress(TEST_REQUEST_ID, 1.0, "Tour generation complete");

        assertThat(progressListener.getProgressForDevice(TEST_REQUEST_ID, TEST_VISITOR_ID)).isEmpty();
    }

    @Test
    void whenErrorOccurs_thenTracksErrorAndRemovesOnCompletion() {
        progressListener.initializeProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);
        TourGenerationProgress progress = progressListener.getProgressForDevice(TEST_REQUEST_ID, TEST_VISITOR_ID).get();

        progress.setError("Failed to generate tour");
        progressListener.updateProgress(TEST_REQUEST_ID, 1.0, "Error occurred");

        assertThat(progressListener.getProgressForDevice(TEST_REQUEST_ID, TEST_VISITOR_ID)).isEmpty();
    }

    @Test
    void whenUpdatingNonexistentRequest_thenNoException() {
        assertThatCode(() ->
                progressListener.updateProgress("nonexistent", 0.5, "test"))
                .doesNotThrowAnyException();
    }
}