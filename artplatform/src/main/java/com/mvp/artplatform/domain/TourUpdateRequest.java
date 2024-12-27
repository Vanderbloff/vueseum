package com.mvp.artplatform.domain;

import jakarta.validation.constraints.Size;


public record TourUpdateRequest(
        @Size(max = 100)
        String name,

        @Size(max = 2000)
        String description
) {}