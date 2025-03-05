import com.mvp.vueseum.domain.TourGenerationProgress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TourGenerationProgressTest {
    private static final String TEST_REQUEST_ID = "test-request-123";
    private static final String TEST_VISITOR_ID = "test-visitor-456";

    @Test
    void whenCreated_thenInitializedCorrectly() {
        TourGenerationProgress progress = new TourGenerationProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);

        assertThat(progress.getRequestId()).isEqualTo(TEST_REQUEST_ID);
        assertThat(progress.getVisitorId()).isEqualTo(TEST_VISITOR_ID);
        assertThat(progress.getProgress()).isZero();
        assertThat(progress.isHasError()).isFalse();
        assertThat(progress.getErrorMessage()).isNull();
    }

    @Test
    void whenUpdatingProgress_thenValidatesRange() {
        TourGenerationProgress progress = new TourGenerationProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);

        assertThatCode(() ->
                progress.update(0.5, "Valid progress"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() ->
                progress.update(-0.1, "Invalid progress"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Progress must be between 0.0 and 1.0");

        assertThatThrownBy(() ->
                progress.update(1.1, "Invalid progress"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Progress must be between 0.0 and 1.0");
    }

    @Test
    void whenSettingError_thenUpdatesErrorState() {
        TourGenerationProgress progress = new TourGenerationProgress(TEST_REQUEST_ID, TEST_VISITOR_ID);
        String errorMessage = "Test error message";

        progress.setError(errorMessage);

        assertThat(progress.isHasError()).isTrue();
        assertThat(progress.getErrorMessage()).isEqualTo(errorMessage);
    }
}