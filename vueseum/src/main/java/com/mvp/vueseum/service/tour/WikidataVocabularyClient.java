package com.mvp.vueseum.service.tour;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class WikidataVocabularyClient {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public WikidataVocabularyClient(HttpClient client) {
        this.client = client;
    }

    public WikidataVocabularyClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Find exact match for an art medium term in Wikidata.
     *
     * @param term The term to find
     * @return Optional containing the standardized term if found
     */
    public Optional<String> findExactArtMediumMatch(String term) {
        try {
            if (term == null || term.isBlank()) {
                return Optional.empty();
            }

            String sparqlQuery = buildExactMatchQuery(term);

            String encodedQuery = URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8);
            String url = "https://query.wikidata.org/sparql?query=" + encodedQuery + "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/sparql-results+json")
                    .header("User-Agent", "VueseumApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode bindings = root.path("results").path("bindings");

                if (!bindings.isEmpty()) {
                    JsonNode binding = bindings.get(0);
                    String itemLabel = binding.path("itemLabel").path("value").asText();

                    // Ensure proper capitalization
                    return Optional.of(
                            Character.toUpperCase(itemLabel.charAt(0)) +
                                    itemLabel.substring(1)
                    );
                }
            } else {
                log.warn("Wikidata API returned status code: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("Exception in Wikidata lookup for term '{}': {}", term, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Standardize a cultural term using Wikidata.
     * Uses a two-stage approach:
     * 1. First tries country-to-demonym lookup for modern nationalities
     * 2. Falls back to nationality/ethnic group search for historical terms
     *
     * @param term The cultural term to standardize
     * @return Optional containing the standardized term if found
     */
    public Optional<String> standardizeCulturalTerm(String term) {
        if (term == null || term.isBlank()) {
            return Optional.empty();
        }

        String cleanTerm = removeQualifiers(term);

        log.debug("Attempting to standardize cultural term: '{}' (cleaned: '{}')", term, cleanTerm);

        Optional<String> result = standardizeUsingCountryDemonyms(cleanTerm);
        if (result.isEmpty()) {
            log.debug("Demonym approach failed, trying nationality search for: '{}'", cleanTerm);
            result = standardizeUsingNationalitySearch(cleanTerm);
        }

        return result;
    }

    /**
     * Standardize a cultural term using the country-to-demonym approach.
     * Finds countries with matching demonyms or names and returns the standardized demonym.
     *
     * @param term The term to standardize (should already be cleaned of qualifiers)
     * @return Optional containing the standardized demonym if found
     */
    private Optional<String> standardizeUsingCountryDemonyms(String term) {
        try {
            // Take just first word for better matching with simple nationality terms
            String firstWord = term.toLowerCase().split("\\s+")[0];

            // SPARQL query to find countries with matching demonyms or names
            String sparqlQuery = buildCountryDemonymQuery(firstWord);

            // Execute the query
            JsonNode results = executeWikidataQuery(sparqlQuery);
            if (results == null) {
                return Optional.empty();
            }

            JsonNode bindings = results.path("results").path("bindings");
            if (bindings.isEmpty()) {
                return Optional.empty();
            }

            // Use the first result's demonym as our standardized term
            JsonNode binding = bindings.get(0);
            String demonym = binding.path("demonymEN").path("value").asText();

            // Clean up the standardized term (remove any annotations in parentheses)
            demonym = demonym.replaceAll("\\s+\\([^)]*\\)", "");

            // Ensure first letter is capitalized
            String standardized = Character.toUpperCase(demonym.charAt(0)) + demonym.substring(1);

            log.debug("Successfully standardized via country demonym: '{}' → '{}'", term, standardized);
            return Optional.of(standardized);
        } catch (Exception e) {
            log.error("Exception in demonym standardization for term '{}': {}", term, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Standardize a cultural term using direct nationality/ethnic group search.
     * This is a fallback for historical or complex terms that don't map to modern countries.
     *
     * @param term The term to standardize (should already be cleaned of qualifiers)
     * @return Optional containing the standardized cultural term if found
     */
    private Optional<String> standardizeUsingNationalitySearch(String term) {
        try {
            // SPARQL query for nationalities and ethnic groups
            String sparqlQuery = buildNationalityEthnicGroupQuery(term);

            // Execute the query
            JsonNode results = executeWikidataQuery(sparqlQuery);
            if (results == null) {
                return Optional.empty();
            }

            JsonNode bindings = results.path("results").path("bindings");
            if (bindings.isEmpty()) {
                return Optional.empty();
            }

            // Use the first result's label as our standardized term
            JsonNode binding = bindings.get(0);
            String itemLabel = binding.path("itemLabel").path("value").asText();

            // Ensure first letter is capitalized
            String standardized = Character.toUpperCase(itemLabel.charAt(0)) + itemLabel.substring(1);

            log.debug("Successfully standardized via nationality search: '{}' → '{}'", term, standardized);
            return Optional.of(standardized);
        } catch (Exception e) {
            log.error("Exception in nationality search for term '{}': {}", term, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Execute a SPARQL query against the Wikidata endpoint.
     *
     * @param sparqlQuery The SPARQL query to execute
     * @return JsonNode containing the results, or null if there was an error
     */
    private JsonNode executeWikidataQuery(String sparqlQuery) {
        try {
            String encodedQuery = URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8);
            String url = "https://query.wikidata.org/sparql?query=" + encodedQuery + "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/sparql-results+json")
                    .header("User-Agent", "VueseumApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readTree(response.body());
            } else {
                log.warn("Wikidata API returned status code: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception executing Wikidata query: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Remove qualifier phrases from a term.
     *
     * @param term The term to clean
     * @return The cleaned term without qualifiers
     */
    private String removeQualifiers(String term) {
        return term.toLowerCase()
                .replaceAll("(?i)probably |possibly |perhaps |attributed to ", "")
                .trim();
    }

    /**
     * Build a SPARQL query for exact term matching.
     *
     * @param term The term to match
     * @return SPARQL query string
     */
    private static @NotNull String buildExactMatchQuery(String term) {
        String cleanTerm = term.trim().toLowerCase();

        // Direct SPARQL query to find exact match
        return "SELECT ?item ?itemLabel ?itemDescription WHERE {\n" +
                "  ?item rdfs:label \"" + cleanTerm + "\"@en.\n" +
                "  \n" +
                "  SERVICE wikibase:label {\n" +
                "    bd:serviceParam wikibase:language \"en\".\n" +
                "    ?item rdfs:label ?itemLabel.\n" +
                "    ?item schema:description ?itemDescription.\n" +
                "  }\n" +
                "}";
    }

    /**
     * Build a SPARQL query for the country demonym approach.
     *
     * @param term The term to match
     * @return SPARQL query string
     */
    private static @NotNull String buildCountryDemonymQuery(String term) {
        return "SELECT ?country ?countryLabel ?demonymEN ?demonymForm WHERE {\n" +
                "  # Find countries that have demonyms containing our search term\n" +
                "  ?country wdt:P31 wd:Q6256 .\n" + // Instance of country
                "  ?country wdt:P1549 ?demonym .\n" + // Demonym property
                "  FILTER(LANG(?demonym) = \"en\" || LANG(?demonym) = \"\")\n" + // English demonyms
                "  BIND(LCASE(STR(?demonym)) AS ?demonymLC)\n" + // Lowercase demonym
                "  FILTER(CONTAINS(?demonymLC, \"" + term + "\"))\n" + // Match our term
                "  BIND(STR(?demonym) AS ?demonymEN)\n" + // Store the English demonym
                "  # Or find countries whose English name contains our search term\n" +
                "  OPTIONAL {\n" +
                "    ?country rdfs:label ?countryName .\n" +
                "    FILTER(LANG(?countryName) = \"en\")\n" +
                "    FILTER(CONTAINS(LCASE(STR(?countryName)), \"" + term + "\"))\n" +
                "  }\n" +
                "  # Get proper grammatical forms if available\n" +
                "  OPTIONAL {\n" +
                "    ?country wdt:P5243 ?form .\n" + // Grammatical form property
                "    FILTER(CONTAINS(LCASE(STR(?form)), \"singular\"))\n" +
                "    BIND(STR(?form) AS ?demonymForm)\n" +
                "  }\n" +
                "  # Get the country label\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
                "}\n" +
                "LIMIT 5";
    }

    /**
     * Build a SPARQL query for the nationality/ethnic group approach.
     *
     * @param term The term to match
     * @return SPARQL query string
     */
    private static @NotNull String buildNationalityEthnicGroupQuery(String term) {
        return "SELECT ?item ?itemLabel ?description WHERE {\n" +
                "  SERVICE wikibase:mwapi {\n" +
                "    bd:serviceParam wikibase:api \"EntitySearch\".\n" +
                "    bd:serviceParam wikibase:endpoint \"www.wikidata.org\".\n" +
                "    bd:serviceParam mwapi:search \"" + term + "\".\n" +
                "    bd:serviceParam mwapi:language \"en\".\n" +
                "    ?item wikibase:apiOutputItem mwapi:item.\n" +
                "  }\n" +
                "  {\n" +
                "    ?item wdt:P31/wdt:P279* wd:Q231002 .\n" + // Instance of (subclass of) nationality
                "  } UNION {\n" +
                "    ?item wdt:P31/wdt:P279* wd:Q41710 .\n" + // Instance of (subclass of) ethnic group
                "  } UNION {\n" +
                "    ?item wdt:P31/wdt:P279* wd:Q1541001 .\n" + // Instance of cultural group
                "  }\n" +
                "  # Get descriptions for context\n" +
                "  OPTIONAL { ?item schema:description ?description FILTER(LANG(?description) = \"en\") }\n" +
                "  # Get the item label\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
                "}\n" +
                "LIMIT 5";
    }
}