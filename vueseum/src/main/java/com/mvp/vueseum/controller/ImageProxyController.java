package com.mvp.vueseum.controller;

import com.mvp.vueseum.service.ImageProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
@RequestMapping("/api/v1/images")
public class ImageProxyController {

    private final ImageProxyService imageProxyService;

    public ImageProxyController(ImageProxyService imageProxyService) {
        this.imageProxyService = imageProxyService;
    }

    /**
     * Proxies an image from the specified URL to the client.
     * Uses streaming to avoid excessive memory usage.
     */
    @GetMapping("/proxy")
    public void proxyImage(@RequestParam String url, HttpServletResponse response) {
        try {
            // Decode URL to handle encoded characters
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

            // Stream the image directly to client
            imageProxyService.streamImage(decodedUrl, response);
        } catch (Exception e) {
            log.error("Error processing proxy request for URL: {}", url, e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }
}