package com.app.features.media.validation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.exception.InvalidMediaContentException;
import com.app.features.media.storage.MediaFilenameSupport;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaTypePolicyResolver {

    private final AppProperties appProperties;

    private Map<String, AllowedMediaType> policiesByExtension;

    @PostConstruct
    void initialize() {
        Map<String, AllowedMediaType> policies = new HashMap<>();
        for (AllowedMediaType policy : appProperties.getMedia().getAllowedTypes()) {
            String extension = policy.getExtension().toLowerCase(Locale.ROOT);
            if (policies.put(extension, policy) != null) {
                throw new IllegalStateException("Duplicate media extension policy: " + extension);
            }
        }
        policiesByExtension = Map.copyOf(policies);
    }

    public AllowedMediaType resolve(String originalFilename) {
        String normalizedFilename = MediaFilenameSupport.normalize(originalFilename);
        String extension = MediaFilenameSupport.extensionOf(normalizedFilename);
        AllowedMediaType policy = policiesByExtension.get(extension);

        if (policy == null) {
            throw ExceptionFactory.invalidParam("Unsupported media extension: " + extension);
        }

        return policy;
    }

    public String validateContentType(AllowedMediaType policy, String detectedContentType) {
        String normalizedContentType = normalizeContentType(detectedContentType);
        boolean allowed = policy.getContentTypes().stream()
                .map(contentType -> normalizeContentType(contentType))
                .anyMatch(contentType -> contentType.equals(normalizedContentType));

        if (!allowed) {
            throw new InvalidMediaContentException(
                    "Detected media type does not match the file extension.");
        }

        return normalizedContentType;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidMediaContentException(
                    "Unable to detect media content type.");
        }

        int parameterIndex = contentType.indexOf(';');
        String normalized = parameterIndex >= 0
                ? contentType.substring(0, parameterIndex)
                : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }
}
