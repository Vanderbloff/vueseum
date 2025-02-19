package com.mvp.vueseum.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/api/v1/proxy")
public class ImageProxyController {

    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(url)
                    .build();

            // Using RestClient's more modern API
            byte[] imageData = restClient.get()
                    .retrieve()
                    .body(byte[].class);

            // Response building remains the same
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            return ResponseEntity.notFound().build();
        }
    }
}
