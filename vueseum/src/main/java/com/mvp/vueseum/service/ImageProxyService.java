package com.mvp.vueseum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@Service
@Slf4j
public class ImageProxyService {

    private final RestClient restClient;

    public ImageProxyService() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultRequest(request -> request.header("Accept", "image/*"))
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) ->
                        log.warn("Error response: {} for URL: {}", response.getStatusCode(), request.getURI()))
                .build();
    }

    /**
     * Stream an image from the source URL directly to the client response,
     * without loading the entire image into memory.
     */
    public void streamImage(String url, HttpServletResponse response) {
        try {
            restClient.get()
                    .uri(url)
                    .exchange((_, clientResponse) -> {
                        try {
                            // Check for successful response
                            if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                // Set content type from response
                                MediaType contentType = clientResponse.getHeaders().getContentType();
                                if (contentType != null) {
                                    response.setContentType(contentType.toString());
                                } else {
                                    response.setContentType("image/jpeg"); // Fallback
                                }

                                // Stream the response body directly to client
                                streamResponseToClient(clientResponse, response);
                            } else {
                                log.warn("Source returned status code: {} for URL: {}",
                                        clientResponse.getStatusCode(), url);
                                response.setStatus(HttpStatus.NOT_FOUND.value());
                            }
                        } catch (IOException e) {
                            log.error("Error processing image stream for URL: {}", url, e);
                            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to proxy image from URL: {}", url, e);
            response.setStatus(HttpStatus.BAD_GATEWAY.value());
        }
    }

    /**
     * Stream data from the source response to the client response using a buffer,
     * never loading the entire file into memory.
     */
    private void streamResponseToClient(ClientHttpResponse source, HttpServletResponse target)
            throws IOException {
        target.setHeader("Cache-Control", "public, max-age=2592000"); // 30 days
        target.setHeader("Expires", getExpiresDateString());

        try (InputStream is = source.getBody();
             OutputStream os = target.getOutputStream()) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            long totalBytes = 0;
            final long MAX_SIZE = 50 * 1024 * 1024; // 50MB limit

            while ((bytesRead = is.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_SIZE) {
                    log.warn("Image exceeded size limit of 50MB");
                    break; // Stop streaming but send what we've got so far
                }
                os.write(buffer, 0, bytesRead);
                os.flush(); // Ensure data is sent immediately
            }
        }
    }

    private String getExpiresDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
        return dateFormat.format(expiresDate);
    }
}