package com.mvp.artplatform.service.visitor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@PropertySource("classpath:visitor.properties")
public class VisitorTrackingService {

    private record VisitorData(
            LocalDateTime lastVisit,
            String deviceFingerprint,
            AtomicInteger todayCount,
            LocalDate countDate  // Track which day the count is for
    ) {
        // Verify if the count is still valid for today
        boolean isCountValid() {
            return countDate.equals(LocalDate.now());
        }
    }

    // Cache configuration for visitor data
    private final Cache<String, VisitorData> visitorCache;

    // Configurable daily generation limit
    private final int dailyGenerationLimit;

    public VisitorTrackingService(
            @Value("${tour.generation.daily-limit}") int dailyGenerationLimit,
            @Value("${tour.visitor.data-retention-days}") int retentionDays) {

        this.dailyGenerationLimit = dailyGenerationLimit;
        // Duration to keep visitor data in cache
        Duration visitorDataRetention = Duration.ofDays(retentionDays);

        // Initialize cache with configuration
        this.visitorCache = Caffeine.newBuilder()
                .expireAfterAccess(visitorDataRetention)
                .maximumSize(100_000)  // Adjust based on expected concurrent visitors
                .build();
    }

    /**
     * Records a tour generation attempt for a visitor.
     * @param visitorId Unique identifier for the visitor
     * @param deviceFingerprint Browser/device fingerprint for tracking returning visitors
     * @return true if the generation was recorded, false if limit exceeded
     */
    public boolean recordTourGeneration(String visitorId, String deviceFingerprint) {
        // Get or create visitor data
        VisitorData currentData = visitorCache.get(visitorId, _ -> createNewVisitorData(visitorId, deviceFingerprint));

        // If this is a new day, reset the count
        if (!currentData.isCountValid()) {
            currentData = createNewVisitorData(visitorId, currentData.deviceFingerprint());
        }

        // Check if we're under the limit
        int currentCount = currentData.todayCount().get();
        if (currentCount >= dailyGenerationLimit) {
            log.debug("Tour generation limit reached for visitor: {}", visitorId);
            return false;
        }

        // Increment count and update cache
        currentData.todayCount().incrementAndGet();
        updateVisitorData(visitorId, currentData);

        log.debug("Recorded generation {} of {} for visitor: {}",
                currentCount + 1, dailyGenerationLimit, visitorId);
        return true;
    }

    /**
     * Checks if this is a returning visitor based on device fingerprint.
     * @param deviceFingerprint Browser/device fingerprint
     * @return true if this is a returning visitor
     */
    public boolean isReturningVisitor(String deviceFingerprint) {
        // Search cache for matching fingerprint
        return visitorCache.asMap()
                .values()
                .stream()
                .anyMatch(data -> data.deviceFingerprint().equals(deviceFingerprint));
    }

    // Private helper methods

    private VisitorData createNewVisitorData(String visitorId, String deviceFingerprint) {
        VisitorData newData = new VisitorData(
                LocalDateTime.now(),
                deviceFingerprint,
                new AtomicInteger(0),
                LocalDate.now()
        );

        visitorCache.put(visitorId, newData); // New visitor will be added to cache or have their data updated
        return newData;
    }

    private void updateVisitorData(String visitorId, VisitorData currentData) {
        // Create new data with updated last visit time
        VisitorData updatedData = new VisitorData(
                LocalDateTime.now(),
                currentData.deviceFingerprint(),
                currentData.todayCount(),
                currentData.countDate()
        );

        visitorCache.put(visitorId, updatedData);
    }
}
