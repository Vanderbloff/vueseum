import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.service.tour.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {
    private ScoringService scoringService;

    private Artwork previousArtwork;
    private Artwork currentArtwork;
    private TourPreferences preferences;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService();

        Artist artist1 = new Artist();
        artist1.setArtistName("Artist 1");
        artist1.setBirthDate("1800");
        artist1.setDeathDate("1870");

        Artist artist2 = new Artist();
        artist2.setArtistName("Artist 2");
        artist2.setBirthDate("1820");
        artist2.setDeathDate("1890");

        previousArtwork = new Artwork();
        previousArtwork.setTitle("Previous Artwork");
        previousArtwork.setArtist(artist1);
        previousArtwork.setCreationDate("1850");
        previousArtwork.setCulture("French");

        currentArtwork = new Artwork();
        currentArtwork.setTitle("Current Artwork");
        currentArtwork.setArtist(artist2);
        currentArtwork.setCreationDate("1860");
        currentArtwork.setCulture("Italian");

        preferences = new TourPreferences();
        preferences.setTheme(Tour.TourTheme.CHRONOLOGICAL);
    }

    @Test
    void whenScoringChronologicalTheme_thenPrioritizesTimelineFlow() {
        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.11, 0.13);
    }

    @Test
    void whenScoringArtistFocusedTheme_thenPrioritizesArtistRelationships() {
        preferences.setTheme(Tour.TourTheme.ARTIST_FOCUSED);

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.11, 0.13);
    }

    @Test
    void whenScoringCulturalTheme_thenPrioritizesCulturalRelationships() {
        preferences.setTheme(Tour.TourTheme.CULTURAL);
        preferences.setPreferredCultures(Set.of("French", "Italian"));

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.21, 0.23);
    }

    @Test
    void whenArtworkHasNullDate_thenReturnsLowerButNonZeroScore() {
        preferences.setTheme(Tour.TourTheme.CHRONOLOGICAL);
        currentArtwork.setCreationDate(null);

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.05, 0.07);
    }

    @Test
    void whenArtworkHasCenturyFormat_thenHandlesDateProperly() {
        preferences.setTheme(Tour.TourTheme.CHRONOLOGICAL);
        currentArtwork.setCreationDate("19th century");
        previousArtwork.setCreationDate("18th century");

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.08, 0.10);
    }

    @Test
    void whenArtistsAreContemporaries_thenScoresHigherForArtistTheme() {
        preferences.setTheme(Tour.TourTheme.ARTIST_FOCUSED);

        Artist artist1 = new Artist();
        artist1.setArtistName("Artist 1");
        artist1.setBirthDate("1800");
        artist1.setDeathDate("1870");

        Artist artist2 = new Artist();
        artist2.setArtistName("Artist 2");
        artist2.setBirthDate("1820");
        artist2.setDeathDate("1890");

        previousArtwork.setArtist(artist1);
        currentArtwork.setArtist(artist2);

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.10, 0.11);
    }

    @Test
    void whenArtworkHasNoArtist_thenReturnsBaseLine() {
        preferences.setTheme(Tour.TourTheme.ARTIST_FOCUSED);
        currentArtwork.setArtist(null);

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.05, 0.07);
    }

    @Test
    void whenCulturesShareRegion_thenScoresHigherThanUnrelatedCultures() {
        preferences.setTheme(Tour.TourTheme.CULTURAL);
        previousArtwork.setCulture("Greek");
        currentArtwork.setCulture("Roman");

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.27, 0.28);
    }

    @Test
    void whenCulturesAreUnrelated_thenScoresLower() {
        preferences.setTheme(Tour.TourTheme.CULTURAL);
        previousArtwork.setCulture("Japanese");
        currentArtwork.setCulture("Maya");

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.09, 0.10);
    }

    @Test
    void whenMultiplePreferencesMatch_thenScoresHigher() {
        preferences.setTheme(Tour.TourTheme.CHRONOLOGICAL);
        preferences.setPreferredArtists(Set.of(currentArtwork.getArtistName()));
        preferences.setPreferredPeriods(Set.of(currentArtwork.getCreationDate()));
        preferences.setPreferredMediums(Set.of("Oil on canvas"));
        currentArtwork.setMedium("Oil on canvas");

        double score = scoringService.scoreArtwork(
                currentArtwork,
                preferences,
                List.of(previousArtwork)
        );

        assertThat(score).isBetween(0.37, 0.39);
    }
}