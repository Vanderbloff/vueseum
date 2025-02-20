import com.mvp.vueseum.client.museum_client.MetMuseumApiClient;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.util.RetryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetMuseumApiClientTest {
    @Mock
    private Environment environment;

    @Mock
    private MuseumService museumService;

    @Mock
    private ArtworkService artworkService;

    @Mock
    private RetryUtil retryUtil;

    private MetMuseumApiClient metMuseumApiClient;

    @BeforeEach
    void setUp() {
        // Setup environment mock for rate limit
        when(environment.getProperty(eq("museum.metropolitan.api.rateLimit"), anyString()))
                .thenReturn("80");

        // Setup necessary museumService mocks
        Museum mockMuseum = new Museum();
        mockMuseum.setId(1L);
        mockMuseum.setName("Metropolitan Museum of Art");
        lenient().when(museumService.findOrCreateMuseum(anyString())).thenReturn(mockMuseum);

        metMuseumApiClient = new MetMuseumApiClient(
                retryUtil,
                environment,
                museumService,
                "https://collectionapi.metmuseum.org/public/collection/v1",
                artworkService
        );
    }

    @Test
    void testArtistDateProcessing() {
        // Sample responses based on real Met API data patterns
        List<String> testResponses = Arrays.asList(
                """
                {
                    "objectID": "1",
                    "artistDisplayName": "Vincent van Gogh",
                    "artistBeginDate": "1853",
                    "artistEndDate": "1890",
                    "GalleryNumber": "222",
                    "primaryImage": "http://example.com/image.jpg",
                    "primaryImageSmall": "http://example.com/image-small.jpg"
                }
                """,
                """
                {
                    "objectID": "2",
                    "artistDisplayName": "Unknown Artist",
                    "artistBeginDate": "",
                    "artistEndDate": "",
                    "GalleryNumber": "222",
                    "primaryImage": "",
                    "primaryImageSmall": ""
                }
                """,
                """
                {
                    "objectID": "3",
                    "artistDisplayName": "Workshop of Rembrandt",
                    "artistBeginDate": "ca. 1606",
                    "artistEndDate": "1669?",
                    "GalleryNumber": "222",
                    "primaryImage": "http://example.com/image.jpg",
                    "primaryImageSmall": ""
                }
                """
        );

        // Test each response
        for (String response : testResponses) {
            ArtworkDetails details = metMuseumApiClient.convertToArtworkDetails(response);

            assertNotNull(details, "Should create ArtworkDetails from response");

            // Verify date processing based on response type
            switch (details.getExternalId()) {
                case "1" -> {
                    // Valid dates case
                    assertEquals("1853", details.getArtistBirthYear(),
                            "Should preserve valid birth year");
                    assertEquals("1890", details.getArtistDeathYear(),
                            "Should preserve valid death year");
                }
                case "2" -> {
                    // Empty dates case
                    assertTrue(details.getArtistBirthYear().isEmpty(),
                            "Should handle empty birth year");
                    assertTrue(details.getArtistDeathYear().isEmpty(),
                            "Should handle empty death year");
                }
                case "3" -> {
                    // Invalid format dates case
                    assertTrue(details.getArtistBirthYear().isEmpty(),
                            "Should handle invalid birth year format");
                    assertTrue(details.getArtistDeathYear().isEmpty(),
                            "Should handle invalid death year format");
                }
            }
        }
    }

    @Test
    void testInvalidJsonResponse() {
        String invalidResponse = "invalid json";

        assertThrows(ApiClientException.class,
                () -> metMuseumApiClient.convertToArtworkDetails(invalidResponse),
                "Should throw ApiClientException for invalid JSON");
    }

    @Test
    void testMissingRequiredFields() {
        String incompleteResponse = """
            {
                "artistDisplayName": "Test Artist"
            }
            """;

        assertThrows(ApiClientException.class,
                () -> metMuseumApiClient.convertToArtworkDetails(incompleteResponse),
                "Should throw ApiClientException for missing required fields");
    }

    @Test
    void testEdgeCaseDateFormats() {
        List<String> edgeCases = Arrays.asList(
                """
                {
                    "objectID": "4",
                    "artistDisplayName": "Test Artist",
                    "artistBeginDate": "1800-1900",
                    "artistEndDate": "20th century",
                    "GalleryNumber": "222"
                }
                """,
                """
                {
                    "objectID": "5",
                    "artistDisplayName": "Test Artist",
                    "artistBeginDate": "active 1850",
                    "artistEndDate": "before 1900",
                    "GalleryNumber": "222"
                }
                """,
                """
                {
                    "objectID": "6",
                    "artistDisplayName": "Test Artist",
                    "artistBeginDate": null,
                    "artistEndDate": null,
                    "GalleryNumber": "222"
                }
                """
        );

        for (String response : edgeCases) {
            ArtworkDetails details = metMuseumApiClient.convertToArtworkDetails(response);
            assertNotNull(details, "Should create ArtworkDetails from response");
            assertTrue(details.getArtistBirthYear().isEmpty(),
                    "Should handle complex date format as empty");
            assertTrue(details.getArtistDeathYear().isEmpty(),
                    "Should handle complex date format as empty");
        }
    }
}