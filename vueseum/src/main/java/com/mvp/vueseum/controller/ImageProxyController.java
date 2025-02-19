package com.mvp.vueseum.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/images")
@Slf4j
public class ImageProxyController {

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        log.info("Received proxy request for URL: {}", url);

        try {
            // First decode the URL to ensure we're working with clean data
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            log.info("Decoded URL: {}", decodedUrl);

            RestClient restClient = RestClient.builder()
                    .baseUrl(decodedUrl)
                    .build();

            byte[] imageData = restClient.get()
                    .retrieve()
                    .body(byte[].class);

            log.info("Successfully retrieved image data, size: {} bytes",
                    imageData != null ? imageData.length : 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
            headers.set("Access-Control-Allow-Origin", "*");

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            return ResponseEntity.notFound().build();
        }
    }
}