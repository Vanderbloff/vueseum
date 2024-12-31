package com.mvp.vueseum.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    // Custom metrics for artwork viewing patterns
    @Bean
    public Timer artworkViewTimer() {
        return Timer.builder("artwork.view.time")
                .description("Time spent viewing individual artworks")
                .register(meterRegistry);
    }

    // These will be important for tour generation
    @Bean
    public Timer tourGenerationTimer() {
        return Timer.builder("tour.generation.time")
                .description("Time taken to generate a tour")
                .register(meterRegistry);
    }

    @Bean
    public Timer artworkSelectionTimer() {
        return Timer.builder("artwork.selection.time")
                .description("Time taken to select artworks for a tour")
                .register(meterRegistry);
    }
}