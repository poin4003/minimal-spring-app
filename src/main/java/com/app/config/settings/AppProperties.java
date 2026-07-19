package com.app.config.settings;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import com.app.features.media.enums.MediaKind;

import jakarta.validation.Valid;
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

        @NotNull
        private Duration deliveryCacheDuration = Duration.ofDays(365);

        @Positive
        private long maxImagePixels = 40_000_000;

        @Valid
        @NotEmpty
        private List<AllowedMediaType> allowedTypes = List.of();

        @Valid
        private final Ffmpeg ffmpeg = new Ffmpeg();
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
