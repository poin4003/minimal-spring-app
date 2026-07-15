package com.app.config.settings;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
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
    public static class Security {
        private List<String> apiPublicPaths = List.of(
                "/api/v1/auth/**");

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
