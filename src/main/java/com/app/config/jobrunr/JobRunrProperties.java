package com.app.config.jobrunr;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "org.jobrunr")
public class JobRunrProperties {

    @Valid
    private final BackgroundJobServer backgroundJobServer = new BackgroundJobServer();

    @Valid
    private final Dashboard dashboard = new Dashboard();

    @Valid
    private final Maintenance maintenance = new Maintenance();

    @Data
    public static class BackgroundJobServer {

        private boolean enabled = true;

        @Positive
        private int workerCount = 2;

        @NotNull
        private Duration deleteSucceededJobsAfter = Duration.ofDays(3);

        @NotNull
        private Duration permanentlyDeleteDeletedJobsAfter = Duration.ofDays(1);

        @AssertTrue(message = "JobRunr succeeded and deleted job retention values must be positive.")
        public boolean isRetentionConfigurationValid() {
            return isPositive(deleteSucceededJobsAfter)
                    && isPositive(permanentlyDeleteDeletedJobsAfter);
        }
    }

    @Data
    public static class Dashboard {

        private boolean enabled;

        @Min(1)
        @Max(65535)
        private int port = 8000;
    }

    @Data
    public static class Maintenance {

        @NotNull
        private Duration failedJobRetention = Duration.ofDays(30);

        @AssertTrue(message = "JobRunr failed job retention must be positive.")
        public boolean isRetentionConfigurationValid() {
            return isPositive(failedJobRetention);
        }
    }

    private static boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
