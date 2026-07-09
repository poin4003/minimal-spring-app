package com.app.features.samplepartner.dto.response;

import lombok.Data;

@Data
public class SampleApiResponse<T> {

    private String message;

    private Integer status;

    private T metadata;
}
