package com.mvp.artplatform.client;

import com.mvp.artplatform.model.Artwork;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;


public abstract class BaseMuseumApiClient implements MuseumApiClient {

    protected final RestClient restClient;
    protected final String baseUrl;

    public BaseMuseumApiClient(String baseUrl) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl;
    }

    /*protected List<Artwork> handleResponse(ResponseEntity<T> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return transformResponseToArtworks(response.getBody());
        }

        return Collections.emptyList();
    }*/

    //protected abstract List<Artwork> transformResponseToArtworks(T apiResponse);
}
