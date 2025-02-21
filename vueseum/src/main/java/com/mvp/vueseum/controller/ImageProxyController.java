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
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(url)
                    .header("Accept", "image/*")  // Request images only
                    .retrieve()
                    .toEntity(byte[].class);

            // Check content type - if HTML received, treat as error
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null && contentType.includes(MediaType.TEXT_HTML)) {
                log.warn("Received HTML response instead of image for URL: {}", url);
                return ResponseEntity.notFound().build();
            }

            assert contentType != null;
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            return ResponseEntity.notFound().build();
        }
    }
}
