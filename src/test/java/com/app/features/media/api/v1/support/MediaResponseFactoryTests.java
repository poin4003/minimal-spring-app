package com.app.features.media.api.v1.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.app.config.settings.AppProperties;
import com.app.features.media.schema.result.MediaDeliveryResult;

class MediaResponseFactoryTests {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void buildsCacheableRangeCompatibleResourceResponse() throws Exception {
        Path path = Files.writeString(temporaryDirectory.resolve("image.jpg"), "content");
        AppProperties appProperties = new AppProperties();
        appProperties.getMedia().setDeliveryCacheDuration(Duration.ofHours(1));
        MediaResponseFactory responseFactory = new MediaResponseFactory(appProperties);
        MediaDeliveryResult result = new MediaDeliveryResult(
                path,
                "image/jpeg",
                "image.jpg",
                false);

        ResponseEntity<Resource> response = responseFactory.build(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getCacheControl())
                .contains("max-age=3600", "public", "immutable");
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFile()).isEqualTo(path.toFile());
    }

    @Test
    void buildsRevalidatedResponseForMutableArtifact() throws Exception {
        Path path = Files.writeString(temporaryDirectory.resolve("thumbnail.jpg"), "content");
        MediaResponseFactory responseFactory = new MediaResponseFactory(new AppProperties());
        MediaDeliveryResult result = new MediaDeliveryResult(
                path,
                "image/jpeg",
                null,
                false,
                false);

        ResponseEntity<Resource> response = responseFactory.build(result);

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
    }
}
