package com.mvp.vueseum.service.visitor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@PropertySource("classpath:visitor.properties")
public class VisitorTrackingService {

    @Getter
    private static class VisitorData {
        private final String deviceFingerprint;
        private volatile LocalDateTime lastVisit;
        private final AtomicInteger numOfGeneratedTours;
        private volatile LocalDate latestDay;

        public VisitorData(String deviceFingerprint, Clock clock) {
            if (clock == null) {
                throw new IllegalArgumentException("Clock cannot be null");
            }
            this.deviceFingerprint = deviceFingerprint;
            this.lastVisit = LocalDateTime.now(clock);
            this.numOfGeneratedTours = new AtomicInteger(0);
            this.latestDay = LocalDate.now(clock);
        }

        public synchronized void checkIfDayIsCurrentOrReset(Clock clock) {
            LocalDate today = LocalDate.now(clock);
            if (!today.equals(latestDay)) {
                numOfGeneratedTours.set(0);
                latestDay = today;
            }
        }

        public boolean incrementCount(int limit, Clock clock) {
            // First validate the day and handle resets
            checkIfDayIsCurrentOrReset(clock);

            // Then check if we're at limit
            int currentNumOfTours = numOfGeneratedTours.get();
            if (currentNumOfTours >= limit) {
                return false;
            }

            // Finally increment and update timestamp
            numOfGeneratedTours.incrementAndGet();
            lastVisit = LocalDateTime.now(clock);
            return true;
        }
    }

    // Cache configuration for visitor data
    private final Cache<String, VisitorData> visitorCache;

    // Configurable daily generation limit
    private final int dailyGenerationLimit;
    private final Clock clock;

    public VisitorTrackingService(
            @Value("${tour.generation.daily-limit}") int dailyGenerationLimit,
            @Value("${tour.visitor.data-retention-days}") int retentionDays,
            Clock clock) {

        this.dailyGenerationLimit = dailyGenerationLimit;
        // Duration to keep visitor data in cache
        Duration visitorDataRetention = Duration.ofDays(retentionDays);
        this.clock = clock;

        this.visitorCache = Caffeine.newBuilder()
                .expireAfterAccess(visitorDataRetention)
                .maximumSize(100_000)
                .build();
    }

    public boolean recordTourGeneration(String visitorId) {
        VisitorData data = visitorCache.get(visitorId,
                _ -> new VisitorData(visitorId, clock));

        return data.incrementCount(dailyGenerationLimit, clock);
    }
}
