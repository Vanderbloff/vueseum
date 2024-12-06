package com.mvp.artplatform.client;

import com.mvp.artplatform.dto.ArtworkDetails;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;


public abstract class BaseMuseumApiClient implements MuseumApiClient {

    protected final RestClient restClient;
    protected final Environment environment;
    protected final String baseUrl;

    public BaseMuseumApiClient(Environment environment, String baseUrl) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    protected abstract ArtworkDetails convertToArtworkDetails(String apiResponse);
}
