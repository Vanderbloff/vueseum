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
import java.util.Set;
import java.util.concurrent.TimeUnit;

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


    @Bean
    public Cache<String, Set<Long>> recentlyUsedArtworkCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(7, TimeUnit.DAYS)
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<String, String> deviceFingerprintCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofDays(7))
                .maximumSize(10_000)
                .build();
    }

    @Bean
    public Cache<String, String> standardizedTermCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(12))
                .maximumSize(2000)
                .build();
    }

    private <K, V> Cache<K, V> createCache(Duration expiration, int maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(expiration)
                .maximumSize(maxSize)
                .build();
    }
}
