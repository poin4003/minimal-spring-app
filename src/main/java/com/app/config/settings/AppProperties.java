package com.app.config.settings;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.features.ai.onnx.enums.OnnxExecutionProvider;
import com.app.features.media.enums.HlsReservedVariantKey;
import com.app.features.media.enums.MediaHardwareAccel;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaVideoEncoder;

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

    @Valid
    private final AiSettings ai = new AiSettings();
    private final Auth auth = new Auth();
    private final CacheSettings cache = new CacheSettings();
    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Media media = new Media();
    private final NotificationSettings notification = new NotificationSettings();
    @Valid
    private final PostSettings post = new PostSettings();
    private final Security security = new Security();
    private final Ui ui = new Ui();

    @Data
    public static class AiSettings {
        @Valid
        private final OnnxSettings onnx = new OnnxSettings();

        @Valid
        private final AiGenerationSettings generation =
                new AiGenerationSettings();

        @Valid
        private final EmbeddingSettings embedding = new EmbeddingSettings();

        @Valid
        private final SearchSettings search = new SearchSettings();

        @Valid
        private final VisionSettings vision = new VisionSettings();
    }

    @Data
    public static class AiGenerationSettings {
        private boolean enabled;

        @Valid
        private final AiGenerationMachine machine =
                new AiGenerationMachine();
    }

    @Data
    public static class AiGenerationMachine {
        @NotBlank
        private String modelId = "tjake/Qwen2.5-0.5B-Instruct-JQ4";

        @NotBlank
        private String modelDirectory = "./data/ai-models/jlama";

        @Positive
        private int threads = 4;

        @Positive
        private int maxConcurrency = 1;

        @NotNull
        private Duration timeout = Duration.ofSeconds(120);

        @AssertTrue(message = "AI generation timeout must be positive.")
        public boolean isTimeoutValid() {
            return timeout != null
                    && !timeout.isZero()
                    && !timeout.isNegative();
        }
    }

    @Data
    public static class EmbeddingSettings {
        private boolean enabled;

        @Valid
        private final EmbeddingModel model = new EmbeddingModel();

        @Valid
        private final EmbeddingMachine machine = new EmbeddingMachine();
    }

    @Data
    public static class EmbeddingModel {
        @NotBlank
        private String id = "intfloat/multilingual-e5-small";

        @Pattern(regexp = "[0-9a-f]{40}")
        private String revision =
                "03415a4be176a1620747c692ed433219fabc3def";

        @Pattern(regexp = "[0-9a-f]{64}")
        private String sha256 =
                "ca456c06b3a9505ddfd9131408916dd79290368331e7d76bb621f1cba6bc8665";

        @NotBlank
        private String file = "onnx/model.onnx";

        @Pattern(regexp = "[0-9a-f]{64}")
        private String tokenizerSha256 =
                "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39";

        @NotBlank
        private String tokenizerFile = "onnx/tokenizer.json";
    }

    @Data
    public static class OnnxSettings {
        private boolean fallbackToCpu = true;

        @Valid
        private final OnnxCudaSettings cuda = new OnnxCudaSettings();
    }

    @Data
    public static class OnnxCudaSettings {
        @Min(0)
        private int deviceId;

        @Positive
        private long memoryLimitMb = 2048;
    }

    @Data
    public static class OnnxMachine {
        @NotBlank
        private String modelDirectory = "./data/ai-models/onnx";

        @Positive
        private int threads = 4;

        @Positive
        private int maxConcurrency = 1;

        @NotNull
        private OnnxExecutionProvider executionProvider =
                OnnxExecutionProvider.CPU;
    }

    public static class EmbeddingMachine extends OnnxMachine {
    }

    @Data
    public static class SearchSettings {
        private boolean enabled;

        @NotBlank
        private String indexDirectory = "./data/search/post-index";

        @Positive
        private int defaultLimit = 10;

        @Positive
        @Max(100)
        private int maxLimit = 50;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private float minimumScore;

        @Positive
        @Max(1000)
        private int reconciliationBatchSize = 100;

        @AssertTrue(message = "AI search default limit must not exceed its maximum limit.")
        public boolean isDefaultLimitValid() {
            return defaultLimit <= maxLimit;
        }
    }

    @Data
    public static class VisionSettings {
        private boolean enabled;

        @Valid
        private final VisionModel model = new VisionModel();

        @Valid
        private final VisionMachine machine = new VisionMachine();
    }

    @Data
    public static class VisionModel {
        @NotBlank
        private String id = "openai/clip-vit-base-patch32";

        @Pattern(regexp = "[0-9a-f]{40}")
        private String revision =
                "12b36594d53414ecfba93c7200dbb7c7db3c900a";

        @Pattern(regexp = "[0-9a-f]{64}")
        private String sha256 =
                "57879bb1c23cdeb350d23569dd251ed4b740a96d747c529e94a2bb8040ac5d00";

        @NotBlank
        private String file = "onnx/model.onnx";
    }

    public static class VisionMachine extends OnnxMachine {
    }

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
        private int shortEdge;

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

        @Valid
        private final Machine machine = new Machine();
    }

    @Data
    public static class Machine {
        @NotNull
        private MediaVideoEncoder videoEncoder = MediaVideoEncoder.AUTO;

        @Min(0)
        private int threads = 0;

        @NotNull
        private MediaHardwareAccel hardwareAccel = MediaHardwareAccel.NONE;

        private boolean fallbackToSoftware = true;

        @NotNull
        private Set<String> softwareDecodeCodecs = Set.of("av1");

        private String libvaDriverName = "iHD";
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
    public static class PostSettings {
        @Valid
        private final PostMaintenance maintenance = new PostMaintenance();

        @Valid
        private final ShortPostSettings shortPost = new ShortPostSettings();

        @Valid
        private final AiModerationSettings aiModeration = new AiModerationSettings();
    }

    @Data
    public static class ShortPostSettings {
        @NotEmpty
        private Set<MediaKind> allowedMediaKinds = Set.of(MediaKind.VIDEO);

        @NotNull
        private Duration maxDuration = Duration.ofMinutes(3);

        @DecimalMin(value = "0.01")
        private double minAspectRatio = 0.5;

        @DecimalMin(value = "0.01")
        private double maxAspectRatio = 1.0;

        @Positive
        private int minShortEdge = 720;

        @AssertTrue(message = "Short post policy configuration is invalid.")
        public boolean isPolicyConfigurationValid() {
            return allowedMediaKinds != null
                    && !allowedMediaKinds.isEmpty()
                    && isPositive(maxDuration)
                    && Double.isFinite(minAspectRatio)
                    && Double.isFinite(maxAspectRatio)
                    && minAspectRatio > 0
                    && minAspectRatio <= maxAspectRatio
                    && minShortEdge > 0;
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class AiModerationSettings {
        private boolean enabled;

        @Valid
        private final AiModerationMachine machine = new AiModerationMachine();
    }

    @Data
    public static class AiModerationMachine {
        @NotBlank
        private String mediaBaseUrl = "http://127.0.0.1:8080";

        @Min(0)
        private int maxImages = 1;

        @Positive
        private int maxTokens = 128;
    }

    @Data
    public static class PostMaintenance {
        @NotNull
        private Duration deletedRetention = Duration.ofDays(3);

        @NotNull
        private Duration rejectedRetention = Duration.ofDays(3);

        @AssertTrue(message = "Post retention values must be positive.")
        public boolean isRetentionConfigurationValid() {
            return isPositive(deletedRetention)
                    && isPositive(rejectedRetention);
        }

        private boolean isPositive(Duration value) {
            return value != null && !value.isZero() && !value.isNegative();
        }
    }

    @Data
    public static class Security {
        private List<String> apiPublicPaths = List.of(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/register/**",
                "/api/v1/auth/password-reset/**",
                "/api/v1/public/callback",
                "/api/v1/public/media/**");

        private List<String> webPublicPaths = List.of(
                "/",
                "/posts",
                "/posts/**",
                "/videos",
                "/videos/**",
                "/login",
                "/register",
                "/register/**",
                "/forgot-password",
                "/forgot-password/**",
                "/logout",
                "/error",
                "/favicon.ico",
                "/vendor/**",
                "/swagger-ui.html",
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
        private String feedPath = "/posts";

        @NotBlank
        private String shortsPath = "/shorts";

        @NotBlank
        private String videosPath = "/videos";

        @NotBlank
        private String searchPath = "/search";

        @NotBlank
        private String myPostsPath = "/my/posts";

        @NotBlank
        private String myShortsPath = "/my/shorts";

        @NotBlank
        private String myVideosPath = "/my/videos";

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
        private String applicationTitle = "Motumo";

        @NotBlank
        private String socialTitle = "Motumo";
    }
}
