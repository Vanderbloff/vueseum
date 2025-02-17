package com.mvp.vueseum.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for Vueseum.
 * This class configures CORS (Cross-Origin Resource Sharing) and CSRF (Cross-Site Request Forgery) protection.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
    private final CorsProperties corsProperties;
    private final AdminProperties adminProperties;

    /**
     * Configures the main security filter chain for the application.
     * This determines how requests are secured and what protections are in place.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                // CSRF protection configuration
                .csrf(csrf -> csrf
                        // Disable CSRF for public endpoints and GET requests
                        //  are "safe" methods that don't modify data
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/v1/public/**"),
                                request -> request.getMethod().equals("GET")
                        )
                        // Store CSRF token in a cookie that frontend can read
                        // This cookie will be sent with each request
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                // Configure which URLs are accessible
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )

                .httpBasic(Customizer.withDefaults())

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny
                        )

                        // Content Security Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' https://cdnjs.cloudflare.com; " + // We need this for our artifacts
                                                "img-src 'self' data: /api/placeholder/; " +         // We need this for placeholders
                                                "style-src 'self' 'unsafe-inline';"                  // Required for Tailwind
                                )
                        )

                        // HSTS - Force HTTPS
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000) // 1 year
                        )

                        // Referrer Policy
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )

                        // Permissions Policy (modern implementation)
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "geolocation=(), " +
                                        "camera=(), " +
                                        "microphone=(), " +
                                        "payment=(), " +
                                        "usb=(), " +
                                        "bluetooth=()"
                        ))
                );

        return http.build();
    }

    /**
     * Configures CORS - Cross-Origin Resource Sharing
     * CORS is a security feature that restricts which domains can access your API
     * Without proper CORS configuration, browsers will block requests from your frontend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Set which origins can access the API
        configuration.setAllowedOrigins(Arrays.asList(corsProperties.getCors().allowedOrigins()));

        // Set which HTTP methods are allowed (GET, POST, etc.)
        configuration.setAllowedMethods(Arrays.asList(corsProperties.getCors().allowedMethods()));

        // Set which headers are allowed in requests
        configuration.setAllowedHeaders(Arrays.asList(corsProperties.getCors().allowedHeaders()));

        // How long the browser should cache the CORS response
        configuration.setMaxAge(corsProperties.getCors().maxAge());

        // Allow credentials (cookies, authorization headers)
        // This is needed for CSRF tokens
        configuration.setAllowCredentials(true);

        // Apply this CORS configuration to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminProperties.getUsername())
                .password(passwordEncoder().encode(adminProperties.getPassword()))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}