package com.app.config.settings;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import com.app.features.media.enums.HlsReservedVariantKey;
import com.app.features.media.enums.MediaKind;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Media media = new Media();
    private final Security security = new Security();
    private final Ui ui = new Ui();

    @Data
    public static class Auth {
        private final Cookie cookie = new Cookie();
    }

    @Data
    public static class Cookie {
        @NotBlank
        private String accessTokenName = "ACCESS_TOKEN";

        @NotBlank
        private String refreshTokenName = "REFRESH_TOKEN";

        @NotBlank
        private String path = "/";

        @NotBlank
        private String sameSite = "Lax";

        private boolean secure = false;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class Jwt {
        @NotBlank
        private String secretKey;

        @Positive
        private long accessTokenExpirationMs;

        @Positive
        private long refreshTokenExpirationMs;
    }

    @Data
    public static class Media {
        @NotBlank
        private String storagePath = "./data/media";

        @NotBlank
        @Pattern(regexp = "^/(?:[^/?#]+/)*[^/?#]+$")
        private String publicPath = "/api/v1/public/media";

        @NotNull
        private Duration deliveryCacheDuration = Duration.ofDays(365);

        @Positive
        private long maxImagePixels = 40_000_000;

        @Valid
        private final Thumbnail thumbnail = new Thumbnail();

        @Valid
        private final ChunkUpload chunkUpload = new ChunkUpload();

        @Valid
        private final MediaMaintenance maintenance = new MediaMaintenance();

        @Valid
        private final Hls hls = new Hls();

        @Valid
        @NotEmpty
        private List<AllowedMediaType> allowedTypes = List.of();

        @Valid
        private final Ffmpeg ffmpeg = new Ffmpeg();

        @AssertTrue(message = "Processing workspace TTL must exceed the FFmpeg timeout.")
        public boolean isProcessingWorkspaceTtlValid() {
            Duration processingWorkspaceTtl = maintenance.getProcessingWorkspaceTtl();
            if (processingWorkspaceTtl == null) {
                return true;
            }

            Duration processTimeout = Duration.ofMinutes(ffmpeg.getProcessTimeoutMinutes());
            return processingWorkspaceTtl.compareTo(processTimeout) > 0;
        }
    }

    @Data
    public static class Thumbnail {
        private boolean enabled = true;

        @Positive
        private int maxWidth = 480;

        @Positive
        private int maxHeight = 480;

        @DecimalMin("0.1")
        @DecimalMax("1.0")
        private double jpegQuality = 0.82;

        @Min(0)
        private int videoFrameSecond = 1;

        private boolean pdfEnabled = true;

        private boolean audioCoverEnabled = true;
    }

    @Data
    public static class ChunkUpload {
        @Positive
        private int chunkSizeBytes = 8 * 1024 * 1024;

        @Positive
        private long directUploadThresholdBytes = 16L * 1024 * 1024;

        @Positive
        private int parallelChunks = 3;

        @NotNull
        private Duration sessionTtl = Duration.ofHours(24);

        @NotNull
        private Duration completedSessionRetention = Duration.ofHours(24);

        @AssertTrue(message = "Chunk upload retention values must be positive.")
        public boolean isRetentionConfigurationValid() {
            return isPositive(sessionTtl) && isPositive(completedSessionRetention);
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class MediaMaintenance {
        @NotNull
        private Duration pendingRecoveryTtl = Duration.ofMinutes(15);

        @NotNull
        private Duration stagingTtl = Duration.ofHours(1);

        @NotNull
        private Duration processingWorkspaceTtl = Duration.ofHours(2);

        @NotNull
        private Duration failedArtifactTtl = Duration.ofHours(24);

        @NotNull
        private Duration orphanDirectoryTtl = Duration.ofHours(24);

        @NotNull
        private Duration missingOriginalAuditTtl = Duration.ofMinutes(10);

        @Positive
        private int batchSize = 100;

        @AssertTrue(message = "Media maintenance TTL values must be positive.")
        public boolean isTtlConfigurationValid() {
            return isPositive(pendingRecoveryTtl)
                    && isPositive(stagingTtl)
                    && isPositive(processingWorkspaceTtl)
                    && isPositive(failedArtifactTtl)
                    && isPositive(orphanDirectoryTtl)
                    && isPositive(missingOriginalAuditTtl);
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class Hls {
        @Positive
        private int audioBitrate = 192_000;

        @Valid
        @NotEmpty
        private List<@NotNull HlsRendition> renditions = List.of();

        @AssertTrue(message = "HLS rendition keys must be unique and must not use reserved keys.")
        public boolean isRenditionConfigurationValid() {
            if (renditions == null) {
                return true;
            }

            Set<String> keys = new HashSet<>();
            Set<String> reservedKeys = Set.of(
                    HlsReservedVariantKey.MASTER.getKey(),
                    HlsReservedVariantKey.AUDIO.getKey());
            for (HlsRendition rendition : renditions) {
                if (rendition == null || rendition.getKey() == null) {
                    continue;
                }
                if (reservedKeys.contains(rendition.getKey()) || !keys.add(rendition.getKey())) {
                    return false;
                }
            }
            return true;
        }
    }

    @Data
    public static class HlsRendition {
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+$")
        private String key;

        @Positive
        private int height;

        @Positive
        private int videoBitrate;

        @Positive
        private int audioBitrate;
    }

    @Data
    public static class AllowedMediaType {
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+$")
        private String extension;

        @NotNull
        private MediaKind kind;

        @Positive
        private long maxFileSizeBytes;

        @NotEmpty
        private List<@NotBlank String> contentTypes = List.of();
    }

    @Data
    public static class Ffmpeg {
        @NotBlank
        private String executable = "ffmpeg";

        @NotBlank
        private String ffprobeExecutable = "ffprobe";

        @Positive
        private int segmentDurationSeconds = 6;

        @Positive
        private int processTimeoutMinutes = 30;
    }

    @Data
    public static class Security {
        private List<String> apiPublicPaths = List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/public/media/**");

        private List<String> webPublicPaths = List.of(
                "/login",
                "/logout",
                "/error",
                "/favicon.ico",
                "/vendor/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/h2-console/**",
                "/css/**",
                "/js/**",
                "/images/**");

        private List<String> csrfIgnorePaths = List.of(
                "/api/**",
                "/h2-console/**",
                "/admin/users/**",
                "/admin/rbac/**",
                "/admin/cronjobs/**");
    }

    @Data
    public static class Ui {
        @NotBlank
        private String loginPath = "/login";

        @NotBlank
        private String homePath = "/admin";

        @NotBlank
        private String loginTitle = "Sign in";

        @NotBlank
        private String logoutPath = "/logout";

        @NotBlank
        private String applicationTitle = "Minimal Spring App";
    }
}
