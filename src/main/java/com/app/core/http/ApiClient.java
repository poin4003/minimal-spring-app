package com.app.core.http;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiClient {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    public <T, R> ApiResponse<R> send(BaseApiRequest<T> request, Class<R> responseType) {
        try {
            String finalUrl = buildUrl(request);
            log.info("Sending Webhook [{}] to URL: {}", request.getMethod(), finalUrl);

            var requestBodySpec = restClient.method(Objects.requireNonNull(request.getMethod()))
                    .uri(Objects.requireNonNull(finalUrl))
                    .contentType(Objects.requireNonNull(request.getContentType()))
                    .headers(h -> {
                        if (request.getHeaders() != null)
                            h.addAll(Objects.requireNonNull(request.getHeaders()));
                    });

            if (request.getPayload() != null) {
                requestBodySpec.body(Objects.requireNonNull(request.getPayload()));
            }

            ResponseEntity<String> response = requestBodySpec.retrieve().toEntity(String.class);

            String bodyStr = response.getBody();
            R data = null;

            if (bodyStr != null && !bodyStr.isBlank() && responseType != Void.class) {
                if (responseType == String.class) {
                    data = responseType.cast(bodyStr);
                } else {
                    data = objectMapper.readValue(bodyStr, responseType);
                }
            }

            return new ApiResponse<>(response.getStatusCode().value(), bodyStr, data, false);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Partner API Error: {} - Body: {}", status, body);

            return new ApiResponse<>(status, body, null, false);

        } catch (ResourceAccessException e) {
            log.error("Network error: {}", e.getMessage());
            return new ApiResponse<>(null, e.getMessage(), null, true);

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return new ApiResponse<>(null, e.getMessage(), null, true);
        }
    }

    public <T, R> ApiResponse<R> send(BaseApiRequest<T> request, TypeReference<R> typeReference) {
        try {
            String finalUrl = buildUrl(request);
            log.info("Sending Webhook [{}] to URL: {}", request.getMethod(), finalUrl);

            var requestBodySpec = restClient.method(Objects.requireNonNull(request.getMethod()))
                    .uri(Objects.requireNonNull(finalUrl))
                    .contentType(Objects.requireNonNull(request.getContentType()))
                    .headers(h -> {
                        if (request.getHeaders() != null)
                            h.addAll(Objects.requireNonNull(request.getHeaders()));
                    });

            if (request.getPayload() != null) {
                requestBodySpec.body(Objects.requireNonNull(request.getPayload()));
            }

            ResponseEntity<String> response = requestBodySpec.retrieve().toEntity(String.class);

            String bodyStr = response.getBody();
            R data = null;

            if (bodyStr != null && !bodyStr.isBlank()) {
                data = objectMapper.readValue(bodyStr, typeReference);
            }

            return new ApiResponse<>(response.getStatusCode().value(), bodyStr, data, false);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Partner API Error: {} - Body: {}", status, body);

            return new ApiResponse<>(status, body, null, false);

        } catch (ResourceAccessException e) {
            log.error("Network error: {}", e.getMessage());
            return new ApiResponse<>(null, e.getMessage(), null, true);

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return new ApiResponse<>(null, e.getMessage(), null, true);
        }
    }

    private String buildUrl(BaseApiRequest<?> request) {
        String url = request.getUrl();
        if (request.getPathParams() != null) {
            for (Map.Entry<String, String> entry : request.getPathParams().entrySet()) {
                url = url.replace(":" + entry.getKey(), entry.getValue());
            }
        }
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            url = UriComponentsBuilder.fromUriString(Objects.requireNonNull(url))
                    .queryParams(request.getQueryParams())
                    .build()
                    .toUriString();
        }

        return url;
    }
}
