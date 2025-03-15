package com.mvp.vueseum.service.cultural;

import com.github.benmanes.caffeine.cache.Cache;
import com.mvp.vueseum.entity.StandardizedTerm;
import com.mvp.vueseum.repository.StandardizedTermRepository;
import com.mvp.vueseum.service.tour.WikidataVocabularyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for standardizing cultural and medium terms using Wikidata Vocabularies.
 * Implements a cache-first approach with database persistence.
 */
@Service
@Slf4j
public class CulturalTermStandardizationService {

    // Category constants for term types
    public static final String CATEGORY_CULTURE = "CULTURE";
    public static final String CATEGORY_MEDIUM = "MEDIUM";

    private final StandardizedTermRepository standardizedTermRepository;
    private final WikidataVocabularyClient wikidataClient;

    // Simple rate limiting to avoid overloading Wikidata API
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final Cache<String, String> standardizedTermCache;

    @Autowired
    public CulturalTermStandardizationService(
            StandardizedTermRepository standardizedTermRepository,
            WikidataVocabularyClient wikidataClient,
            Cache<String, String> standardizedTermCache) {
        this.standardizedTermRepository = standardizedTermRepository;
        this.wikidataClient = wikidataClient;
        this.standardizedTermCache = standardizedTermCache;
    }

    /**
     * Get the standardized version of a culture term.
     *
     * @param rawTerm The raw culture term to standardize
     * @return The standardized term, or the original if no standardization is found
     */
    @Transactional
    public String getStandardizedCultureTerm(String rawTerm) {
        if (rawTerm == null || rawTerm.isBlank()) {
            return rawTerm;
        }

        return getStandardizedTerm(rawTerm, CATEGORY_CULTURE);
    }

    /**
     * Get the standardized version of a medium term.
     *
     * @param rawTerm The raw medium term to standardize
     * @return The standardized term, or the original if no standardization is found
     */
    @Transactional
    public String getStandardizedMediumTerm(String rawTerm) {
        if (rawTerm == null || rawTerm.isBlank()) {
            return rawTerm;
        }

        return getStandardizedTerm(rawTerm, CATEGORY_MEDIUM);
    }

    /**
     * Get the standardized version of a term for a specific category.
     *
     * @param rawTerm The raw term to standardize
     * @param category The category (CULTURE or MEDIUM)
     * @return The standardized term, or the original if no standardization is found
     */
    @Transactional
    public String getStandardizedTerm(String rawTerm, String category) {
        // Create a cache key combining the raw term and category
        String cacheKey = rawTerm + ":" + category;

        // Check in-memory cache first (fastest)
        String cachedTerm = standardizedTermCache.getIfPresent(cacheKey);
        if (cachedTerm != null) {
            log.debug("In-memory cache hit for '{}' ({})", rawTerm, category);
            return cachedTerm;
        }

        // Check database cache (slower but persistent)
        Optional<StandardizedTerm> existingTerm = standardizedTermRepository
                .findByRawTermAndCategory(rawTerm, category);

        if (existingTerm.isPresent()) {
            updateTermAccessStats(existingTerm.get());
            String standardizedTerm = existingTerm.get().getStandardizedTerm();
            standardizedTermCache.put(cacheKey, standardizedTerm);
            return standardizedTerm;
        }

        // If not in any cache and within rate limits, call Wikidata API
        if (checkRateLimit()) {
            try {
                Optional<String> standardizedTerm;

                // Use appropriate method based on category
                if (CATEGORY_CULTURE.equals(category)) {
                    standardizedTerm = wikidataClient.standardizeCulturalTerm(rawTerm);
                } else if (CATEGORY_MEDIUM.equals(category)) {
                    standardizedTerm = wikidataClient.findExactArtMediumMatch(rawTerm);
                } else {
                    log.warn("Unsupported category: {}", category);
                    return rawTerm;
                }

                if (standardizedTerm.isPresent()) {
                    saveStandardizedTerm(rawTerm, standardizedTerm.get(), category);
                    standardizedTermCache.put(cacheKey, standardizedTerm.get());
                    return standardizedTerm.get();
                }
            } catch (Exception e) {
                log.error("Error standardizing term '{}': {}", rawTerm, e.getMessage());
            }
        } else {
            log.warn("Wikidata API rate limit reached, using original term");
        }

        // If we couldn't get a standardized term, use the original
        return rawTerm;
    }

    /**
     * Find standardized culture terms that start with the given prefix.
     * <p>
     * TODO: Integrate with SuggestionService to optimize cultural term suggestions
     * by querying the standardized terms database directly instead of filtering all terms in memory.
     * This will improve performance as the dataset grows.
     *
     * @param prefix The prefix to match
     * @return List of matching standardized culture terms
     */
    @Transactional(readOnly = true)
    public List<String> findStandardizedCulturesByPrefix(String prefix) {
        return findStandardizedTermsByPrefix(prefix, CATEGORY_CULTURE);
    }

    /**
     * Find standardized medium terms that start with the given prefix.
     * <p>
     * TODO: Integrate with SuggestionService to optimize medium term suggestions
     * by querying the standardized terms database directly instead of filtering all terms in memory.
     * This will improve performance as the standardized terms database grows.
     *
     * @param prefix The prefix to match
     * @return List of matching standardized medium terms
     */
    @Transactional(readOnly = true)
    public List<String> findStandardizedMediumsByPrefix(String prefix) {
        return findStandardizedTermsByPrefix(prefix, CATEGORY_MEDIUM);
    }

    /**
     * Find standardized terms that start with the given prefix.
     * Used for providing optimized suggestions in the UI.
     * <p>
     * TODO: This is a helper method for the prefix matching functions above.
     * Will provide significant performance benefits for suggestion lookups
     * once integrated with SuggestionService.
     *
     * @param prefix The prefix to match
     * @param category The category (CULTURE or MEDIUM)
     * @return List of matching standardized terms
     */
    @Transactional(readOnly = true)
    private List<String> findStandardizedTermsByPrefix(String prefix, String category) {
        if (prefix == null || prefix.length() < 2) {
            return Collections.emptyList();
        }

        // Search for standardized terms that start with this prefix
        return standardizedTermRepository
                .findByStandardizedTermStartingWithAndCategory(prefix, category)
                .stream()
                .map(StandardizedTerm::getStandardizedTerm)
                .distinct()
                .toList();
    }

    /**
     * Updates access statistics for tracking term usage.
     */
    private void updateTermAccessStats(StandardizedTerm term) {
        term.setLastAccessed(LocalDateTime.now());
        term.setAccessCount(term.getAccessCount() + 1);
        standardizedTermRepository.save(term);
    }

    /**
     * Saves a new standardized term to the database.
     */
    private void saveStandardizedTerm(String rawTerm, String standardizedTerm, String category) {
        StandardizedTerm term = new StandardizedTerm();
        term.setRawTerm(rawTerm);
        term.setStandardizedTerm(standardizedTerm);
        term.setCategory(category);
        term.setAccessCount(1);
        term.setLastAccessed(LocalDateTime.now());

        standardizedTermRepository.save(term);
        log.info("Saved standardized term: '{}' → '{}' ({})",
                rawTerm, standardizedTerm, category);
    }

    /**
     * Simple rate limiting to avoid overloading the Wikidata API.
     * Allows MAX_REQUESTS_PER_MINUTE requests per minute.
     *
     * @return true if we can make another request, false if rate limited
     */
    private boolean checkRateLimit() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastResetTime.get();

        // Reset counter every minute
        if (elapsedTime > 60000) {
            requestCount.set(0);
            lastResetTime.set(currentTime);
            return true;
        }

        // Check if we're under the limit
        int count = requestCount.incrementAndGet();
        return count <= MAX_REQUESTS_PER_MINUTE;
    }
}