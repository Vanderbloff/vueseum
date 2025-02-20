import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jakarta.validation.ConstraintViolation;

import static org.assertj.core.api.Assertions.assertThat;

class ArtistValidationTest {
    private Validator validator;
    private Artist artist;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        artist = new Artist();
    }

    @Nested
    @DisplayName("Birth Date Validation")
    class BirthDateValidation {
        @Test
        @DisplayName("should accept valid year format")
        void validBirthYear() {
            artist.setBirthDate("1850");
            Set<ConstraintViolation<Artist>> violations = validator.validate(artist);
            assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("birthDate"))
                    .isEmpty();
        }

        @Test
        @DisplayName("should reject invalid year format")
        void invalidBirthYear() {
            artist.setBirthDate("185");
            Set<ConstraintViolation<Artist>> violations = validator.validate(artist);
            assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("birthDate"))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Lifespan Validation")
    class LifespanValidation {
        @Test
        @DisplayName("should accept valid lifespan")
        void validLifespan() {
            artist.setBirthDate("1850");
            artist.setDeathDate("1920");
            Set<ConstraintViolation<Artist>> violations = validator.validate(artist);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject impossible lifespan")
        void impossibleLifespan() {
            artist.setBirthDate("1850");
            artist.setDeathDate("2000");  // 150 years
            Set<ConstraintViolation<Artist>> violations = validator.validate(artist);
            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Relationship Management")
    class RelationshipManagement {
        @Test
        @DisplayName("should maintain bidirectional relationship when adding artwork")
        void bidirectionalRelationship() {
            Artist artist = new Artist();
            Artwork artwork = new Artwork();

            artist.addArtwork(artwork);

            assertThat(artist.getWorks()).contains(artwork);
            assertThat(artwork.getArtist()).isSameAs(artist);
        }
    }

    @Nested
    @DisplayName("Additional Metadata Tests")
    class AdditionalMetadataTests {
        @Test
        @DisplayName("should store and retrieve additional metadata")
        void additionalMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("awards", Arrays.asList("Award 1", "Award 2"));
            metadata.put("education", "Art School");

            artist.setAdditionalMetadata(metadata);
            assertThat(artist.getAdditionalMetadata())
                    .containsEntry("awards", Arrays.asList("Award 1", "Award 2"))
                    .containsEntry("education", "Art School");
        }
    }

    @Nested
    @DisplayName("Works Management")
    class WorksManagement {
        @Test
        @DisplayName("should prevent duplicate artworks")
        void noDuplicateArtworks() {
            Artwork artwork = new Artwork();
            artist.addArtwork(artwork);
            artist.addArtwork(artwork);

            assertThat(artist.getWorks()).hasSize(1);
        }

        @Test
        @DisplayName("should properly remove artwork")
        void artworkRemoval() {
            Artwork artwork = new Artwork();
            artist.addArtwork(artwork);
            artist.removeArtwork(artwork);

            assertThat(artist.getWorks()).isEmpty();
            assertThat(artwork.getArtist()).isNull();
        }
    }

    @Test
    @DisplayName("should accept empty string dates")
    void emptyStringDates() {
        artist.setBirthDate("");
        artist.setDeathDate("");
        Set<ConstraintViolation<Artist>> violations = validator.validate(artist);
        assertThat(violations).isEmpty();
    }
}