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
        log.debug("Attempting to proxy image from URL: {}", url);
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(url)
                    .build();

            byte[] imageData = restClient.get()
                    .retrieve()
                    .body(byte[].class);

            log.debug("Successfully retrieved image from URL: {}", url);

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
