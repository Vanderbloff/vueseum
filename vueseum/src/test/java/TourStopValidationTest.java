import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourStopValidationTest {
    private Validator validator;
    private TourStop tourStop;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        tourStop = new TourStop();
        tourStop.setTour(new Tour());
        tourStop.setArtwork(new Artwork());
        tourStop.setSequenceNumber(0);
    }

    @Nested
    @DisplayName("Sequence Validation")
    class SequenceValidation {
        @Test
        @DisplayName("should reject negative sequence numbers")
        void negativeSequence() {
            tourStop.setSequenceNumber(-1);

            assertThrows(IllegalStateException.class, () -> {
                tourStop.validateSequence();
            });
        }

        @Test
        @DisplayName("should accept zero sequence number")
        void zeroSequence() {
            tourStop.setSequenceNumber(0);
            Set<ConstraintViolation<TourStop>> violations = validator.validate(tourStop);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Description Management")
    class DescriptionManagement {
        @Test
        @DisplayName("should handle different description types")
        void descriptionTypes() {
            tourStop.setStandardDescription("Standard museum description");
            tourStop.setTourContextDescription("Special tour context");

            assertThat(tourStop.getStandardDescription())
                    .isNotEqualTo(tourStop.getTourContextDescription());
        }

        @Test
        @DisplayName("should handle null descriptions")
        void nullDescriptions() {
            tourStop.setStandardDescription(null);
            tourStop.setTourContextDescription(null);

            Set<ConstraintViolation<TourStop>> violations = validator.validate(tourStop);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Required Stop Tests")
    class RequiredStopTests {
        @Test
        @DisplayName("should track required status")
        void requiredStatus() {
            tourStop.setRequired(true);
            assertThat(tourStop.isRequired()).isTrue();
        }
    }
}