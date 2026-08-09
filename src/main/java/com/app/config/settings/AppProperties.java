package com.app.config.settings;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.features.media.enums.HlsReservedVariantKey;
import com.app.features.media.enums.MediaKind;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final CacheSettings cache = new CacheSettings();
    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Media media = new Media();
    private final NotificationSettings notification = new NotificationSettings();
    private final Security security = new Security();
    private final Ui ui = new Ui();

    @Data
    public static class Auth {
        @NotBlank
        @Size(min = 32)
        private String credentialHashSecret;

        private final Cookie cookie = new Cookie();

        @Valid
        private final Registration registration = new Registration();

        @Valid
        private final PasswordReset passwordReset = new PasswordReset();
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
    public static class Registration {
        @Min(6)
        @Max(8)
        private int otpLength = 6;

        @NotNull
        private Duration otpTtl = Duration.ofMinutes(10);

        @NotNull
        private Duration resendCooldown = Duration.ofMinutes(1);

        @Positive
        private int maxAttempts = 5;

        @NotNull
        private Duration completionTokenTtl = Duration.ofMinutes(20);

        @NotNull
        private Duration cleanupRetention = Duration.ofDays(1);

        @AssertTrue(message = "Registration duration configuration is invalid.")
        public boolean isDurationConfigurationValid() {
            return isPositive(otpTtl)
                    && isPositive(resendCooldown)
                    && isPositive(completionTokenTtl)
                    && isPositive(cleanupRetention)
                    && resendCooldown.compareTo(otpTtl) < 0
                    && cleanupRetention.compareTo(otpTtl) > 0
                    && cleanupRetention.compareTo(completionTokenTtl) > 0;
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class PasswordReset {
        @Min(6)
        @Max(8)
        private int otpLength = 6;

        @NotNull
        private Duration otpTtl = Duration.ofMinutes(10);

        @NotNull
        private Duration resendCooldown = Duration.ofMinutes(1);

        @Positive
        private int maxAttempts = 5;

        @NotNull
        private Duration resetTokenTtl = Duration.ofMinutes(20);

        @NotNull
        private Duration cleanupRetention = Duration.ofDays(1);

        @AssertTrue(message = "Password reset duration configuration is invalid.")
        public boolean isDurationConfigurationValid() {
            return isPositive(otpTtl)
                    && isPositive(resendCooldown)
                    && isPositive(resetTokenTtl)
                    && isPositive(cleanupRetention)
                    && resendCooldown.compareTo(otpTtl) < 0
                    && cleanupRetention.compareTo(otpTtl) > 0
                    && cleanupRetention.compareTo(resetTokenTtl) > 0;
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class CacheSettings {
        @Valid
        private final KeyStoreCache keyStore = new KeyStoreCache();
    }

    @Data
    public static class KeyStoreCache {
        @Positive
        private long maximumSize = 100_000;

        @NotNull
        private Duration ttl = Duration.ofMinutes(1);

        @AssertTrue(message = "Key store cache TTL must be positive.")
        public boolean isTtlValid() {
            return ttl != null && !ttl.isZero() && !ttl.isNegative();
        }
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

        @Positive
        private int maxActiveSessionsPerUser = 5;

        @Positive
        private long maxReservedBytesPerUser = 2L * 1024 * 1024 * 1024;

        @NotNull
        private Duration sessionTtl = Duration.ofHours(24);

        @NotNull
        private Duration maxSessionLifetime = Duration.ofHours(72);

        @NotNull
        private Duration completedSessionRetention = Duration.ofHours(24);

        @AssertTrue(message = "Chunk upload retention values are invalid.")
        public boolean isRetentionConfigurationValid() {
            return isPositive(sessionTtl)
                    && isPositive(maxSessionLifetime)
                    && isPositive(completedSessionRetention)
                    && maxSessionLifetime.compareTo(sessionTtl) >= 0;
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
    public static class NotificationSettings {
        @Valid
        private final NotificationEmail email = new NotificationEmail();

        @Valid
        private final NotificationPolicy policy =
                new NotificationPolicy();

        @Valid
        private final NotificationSse sse = new NotificationSse();

        @Valid
        private final NotificationTelegram telegram =
                new NotificationTelegram();
    }

    @Data
    public static class NotificationEmail {
        private boolean enabled;

        @NotBlank
        @Email
        private String fromAddress = "no-reply@example.com";
    }

    @Data
    public static class NotificationPolicy {
        @NotNull
        private Duration ttl = Duration.ofDays(30);

        @Positive
        private int hardLimitPerUser = 100;

        @AssertTrue(message = "Notification TTL must be positive.")
        public boolean isTtlValid() {
            return ttl != null
                    && !ttl.isZero()
                    && !ttl.isNegative();
        }
    }

    @Data
    public static class NotificationSse {
        @NotNull
        private Duration connectionTimeout = Duration.ofMinutes(30);

        @NotNull
        private Duration heartbeatInterval = Duration.ofSeconds(20);

        @AssertTrue(message = "Notification SSE durations are invalid.")
        public boolean isDurationConfigurationValid() {
            return isPositive(connectionTimeout)
                    && isPositive(heartbeatInterval)
                    && heartbeatInterval.compareTo(connectionTimeout) < 0;
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class NotificationTelegram {
        private boolean enabled;

        @NotBlank
        private String apiBaseUrl = "https://api.telegram.org";

        private String botToken;
        private String groupChatId;

        @AssertTrue(
                message = "Telegram bot token and group chat ID are required when Telegram is enabled.")
        public boolean isProviderConfigurationValid() {
            return !enabled
                    || (StringUtils.hasText(botToken)
                            && StringUtils.hasText(groupChatId));
        }
    }

    @Data
    public static class Security {
        private List<String> apiPublicPaths = List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/register/**",
                "/api/v1/auth/password-reset/**",
                "/api/v1/public/media/**");

        private List<String> webPublicPaths = List.of(
                "/",
                "/login",
                "/register",
                "/register/**",
                "/forgot-password",
                "/forgot-password/**",
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
        private String socialPath = "/";

        @NotBlank
        private String landingPath = "/home";

        @NotBlank
        private String loginPath = "/login";

        @NotBlank
        private String registrationPath = "/register";

        @NotBlank
        private String forgotPasswordPath = "/forgot-password";

        @NotBlank
        private String homePath = "/admin";

        @NotBlank
        private String profilePath = "/profile";

        @NotBlank
        private String notificationPath = "/notifications";

        @NotBlank
        private String logoutPath = "/logout";

        @NotBlank
        private String applicationTitle = "Minimal Spring App";
    }
}
