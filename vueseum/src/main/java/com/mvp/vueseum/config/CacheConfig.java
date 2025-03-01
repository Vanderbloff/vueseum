package com.mvp.vueseum.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.service.tour.FilterOptionsService;
import com.mvp.vueseum.service.tour.SuggestionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {
    @Bean
    public Cache<String, Artwork> artworkCache() {
        return createCache(Duration.ofDays(1), 1000);
    }

    @Bean
    public Cache<String, FilterOptionsService.FilterOptions> filterOptionsCache() {
        return createCache(Duration.ofHours(1), 1000);
    }

    @Bean
    public Cache<String, List<SuggestionService.Suggestion>> suggestionCache() {
        return createCache(Duration.ofMinutes(30), 1000);
    }

    @Bean
    public Cache<String, String> descriptionCache() {
        return createCache(Duration.ofDays(1), 1000);
    }

    @Bean
    public Cache<String, List<String>> filterValueCache() {
        return createCache(Duration.ofHours(24), 1000);
    }

    private <K, V> Cache<K, V> createCache(Duration expiration, int maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(expiration)
                .maximumSize(maxSize)
                .build();
    }

    /**
     * Cache for storing device fingerprints by token
     * Longer expiration to maintain device identity
     */
    @Bean
    public Cache<String, String> deviceFingerprintCache() {
        return createCache(Duration.ofDays(30), 100_000);
    }
}
