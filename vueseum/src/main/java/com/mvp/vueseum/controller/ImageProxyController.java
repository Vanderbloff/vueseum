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
            String decodedUrl = url.contains("%") ?
                    URLDecoder.decode(url, StandardCharsets.UTF_8) : url;
            log.info("Decoded URL: {}", decodedUrl);

            RestClient restClient = RestClient.create();
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(decodedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.metmuseum.org/")
                    .retrieve()
                    .toEntity(byte[].class);

            if (response.getStatusCode().isError() || response.getBody() == null) {
                log.error("Failed to fetch image, status: {}", response.getStatusCode());
                return ResponseEntity.notFound().build();
            }

            byte[] imageData = response.getBody();
            if (imageData.length == 0) {
                log.error("Fetched image is empty");
                return ResponseEntity.notFound().build();
            }

            log.info("Successfully retrieved image, size: {} bytes", imageData.length);

            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null) {
                log.warn("No content type detected, defaulting to JPEG");
                contentType = MediaType.IMAGE_JPEG;
            }

            headers.setContentType(contentType);
            headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS)
                    .cachePublic()
                    .mustRevalidate());
            headers.set("Access-Control-Allow-Origin", "*");

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
