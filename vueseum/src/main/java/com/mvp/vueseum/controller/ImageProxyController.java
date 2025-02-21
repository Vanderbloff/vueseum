package com.mvp.vueseum.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/api/v1/images")
public class ImageProxyController {

    @GetMapping("/proxy")
    @Cacheable(value = "imageCache", key = "#url", unless = "#result.statusCodeValue == 404")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        log.info("Received proxy request for URL: {}", url);

        try {
            // Decode the URL if necessary
            String decodedUrl = url.contains("%") ?
                    URLDecoder.decode(url, StandardCharsets.UTF_8) : url;
            log.info("Decoded URL: {}", decodedUrl);

            RestClient restClient = RestClient.create();
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(decodedUrl)
                    .retrieve()
                    .toEntity(byte[].class);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                log.error("Failed to fetch image, status: {}", response.getStatusCode());
                return ResponseEntity.notFound().build();
            }

            byte[] imageData = response.getBody();
            if (imageData == null || imageData.length == 0) {
                log.error("Fetched image is empty, returning 404.");
                return ResponseEntity.notFound().build();
            }

            log.info("Successfully retrieved image, size: {} bytes", imageData.length);

            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();

            if (contentType == null) {
                log.warn("No content type detected, defaulting to JPEG");
                contentType = MediaType.IMAGE_JPEG; // Default to JPEG if unknown
            }

            headers.setContentType(contentType);
            headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().mustRevalidate());
            headers.set("Access-Control-Allow-Origin", "*");

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            return ResponseEntity.notFound().build();
        }
    }
}
