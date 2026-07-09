package com.app.config.settings;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Security security = new Security();
    private final Mq mq = new Mq();
    private final AsyncTasks asyncTasks = new AsyncTasks();
    private final SystemAdmin systemAdmin = new SystemAdmin();

    @Data
    public static class SystemAdmin {
        @NotBlank
        private String email = "superadmin@app.com";

        @NotBlank
        private String password = "change-me";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class Jwt {
        @Positive
        private long accessTokenExpirationMs;

        @Positive
        private long refreshTokenExpirationMs;
    }

    @Data
    public static class Security {
        @NotNull
        private CryptoAlgo algo = CryptoAlgo.HMAC;

        public enum CryptoAlgo {
            HMAC, RSA
        }
    }

    @Data
    public static class Mq {
        private Retry retry = new Retry();
        private Consumers consumers = new Consumers();

        @Data
        public static class Consumers {
            private ConsumerConfig simImport = new ConsumerConfig();
            // Add more config here
        }

        @Data
        public static class Retry {
            private Integer defaultMaxAttempts = 3;
            private Long defaultBackoffMs = 1000L;
            private Double defaultMultiplier = 1.0;
        }

        @Data
        public static class ConsumerConfig {
            @Positive
            private int concurrency = 1;
        }
    }

    @Data
    public static class AsyncTasks {
        private ExcelTasks excel = new ExcelTasks();

        @Data
        public static class ExcelTasks {
            private ExcelConfig exportSim = new ExcelConfig();
            // Add more config here
        }

        @Data
        public static class ExcelConfig {
            @Positive
            private int chunkRows = 5000;
            @Positive
            private int maxConcurrent = 3;
        }
    }
}
