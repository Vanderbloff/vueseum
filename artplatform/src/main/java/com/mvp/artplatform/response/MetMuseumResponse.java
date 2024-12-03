package com.mvp.artplatform.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetMuseumResponse {

    private List<Integer> objectIds;
    private Integer total;

}
