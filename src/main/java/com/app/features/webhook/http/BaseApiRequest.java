package com.app.features.webhook.http;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseApiRequest<T> {

    private String url;

    @Builder.Default
    private MediaType contentType = MediaType.APPLICATION_JSON;

    @Builder.Default
    private HttpMethod method = HttpMethod.POST;

    private HttpHeaders headers;

    private T payload;

    private Map<String, String> pathParams;

    private MultiValueMap<String, String> queryParams;
}
