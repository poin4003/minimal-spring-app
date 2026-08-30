package com.app.features.ai.benchmark.support;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.app.config.settings.AppProperties;
import com.app.features.ai.onnx.enums.OnnxExecutionProvider;

public final class AiBenchmarkConfiguration {

    private static final Pattern SIMPLE_DURATION =
            Pattern.compile("([0-9]+)(ms|s|m|h)");

    private AiBenchmarkConfiguration() {
    }

    public static AppProperties fromEnvironment() {
        AppProperties properties = new AppProperties();
        Map<String, String> environment = System.getenv();

        AppProperties.OnnxSettings onnxSettings = properties.getAi().getOnnx();
        applyBoolean(
                environment,
                "AI_ONNX_FALLBACK_TO_CPU",
                value -> onnxSettings.setFallbackToCpu(value));
        applyNonNegativeInt(
                environment,
                "AI_ONNX_CUDA_DEVICE_ID",
                value -> onnxSettings.getCuda().setDeviceId(value));
        applyPositiveLong(
                environment,
                "AI_ONNX_CUDA_MEMORY_LIMIT_MB",
                value -> onnxSettings.getCuda().setMemoryLimitMb(value));

        AppProperties.EmbeddingModel embeddingModel = properties.getAi()
                .getEmbedding()
                .getModel();
        AppProperties.EmbeddingMachine embeddingMachine = properties.getAi()
                .getEmbedding()
                .getMachine();
        apply(environment, "AI_EMBEDDING_MODEL_ID", embeddingModel::setId);
        apply(
                environment,
                "AI_EMBEDDING_MODEL_REVISION",
                embeddingModel::setRevision);
        apply(
                environment,
                "AI_EMBEDDING_MODEL_SHA256",
                embeddingModel::setSha256);
        apply(
                environment,
                "AI_EMBEDDING_MODEL_FILE",
                embeddingModel::setFile);
        apply(
                environment,
                "AI_EMBEDDING_TOKENIZER_SHA256",
                embeddingModel::setTokenizerSha256);
        apply(
                environment,
                "AI_EMBEDDING_TOKENIZER_FILE",
                embeddingModel::setTokenizerFile);
        apply(
                environment,
                "AI_EMBEDDING_MODEL_DIRECTORY",
                embeddingMachine::setModelDirectory);
        applyPositiveInt(
                environment,
                "AI_EMBEDDING_THREADS",
                embeddingMachine::setThreads);
        applyPositiveInt(
                environment,
                "AI_EMBEDDING_MAX_CONCURRENCY",
                embeddingMachine::setMaxConcurrency);
        applyExecutionProvider(
                environment,
                "AI_EMBEDDING_EXECUTION_PROVIDER",
                value -> embeddingMachine.setExecutionProvider(value));

        AppProperties.VisionModel visionModel = properties.getAi()
                .getVision()
                .getModel();
        AppProperties.VisionMachine visionMachine = properties.getAi()
                .getVision()
                .getMachine();
        apply(environment, "AI_VISION_MODEL_ID", visionModel::setId);
        apply(
                environment,
                "AI_VISION_MODEL_REVISION",
                visionModel::setRevision);
        apply(
                environment,
                "AI_VISION_MODEL_SHA256",
                visionModel::setSha256);
        apply(environment, "AI_VISION_MODEL_FILE", visionModel::setFile);
        apply(
                environment,
                "AI_VISION_MODEL_DIRECTORY",
                visionMachine::setModelDirectory);
        applyPositiveInt(
                environment,
                "AI_VISION_THREADS",
                visionMachine::setThreads);
        applyPositiveInt(
                environment,
                "AI_VISION_MAX_CONCURRENCY",
                visionMachine::setMaxConcurrency);
        applyExecutionProvider(
                environment,
                "AI_VISION_EXECUTION_PROVIDER",
                value -> visionMachine.setExecutionProvider(value));

        AppProperties.AiGenerationMachine jlamaMachine = properties.getAi()
                .getGeneration()
                .getMachine();
        apply(
                environment,
                "AI_GENERATION_MODEL_ID",
                value -> jlamaMachine.setModelId(value));
        apply(
                environment,
                "AI_GENERATION_MODEL_DIRECTORY",
                value -> jlamaMachine.setModelDirectory(value));
        applyPositiveInt(
                environment,
                "AI_GENERATION_THREADS",
                value -> jlamaMachine.setThreads(value));
        applyPositiveInt(
                environment,
                "AI_GENERATION_MAX_CONCURRENCY",
                value -> jlamaMachine.setMaxConcurrency(value));
        applyDuration(
                environment,
                "AI_GENERATION_TIMEOUT",
                value -> jlamaMachine.setTimeout(value));
        return properties;
    }

    private static void apply(
            Map<String, String> environment,
            String name,
            Consumer<String> setter) {
        String value = environment.get(name);
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }

    private static void applyPositiveInt(
            Map<String, String> environment,
            String name,
            IntConsumer setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(
                        name + " must be greater than zero.");
            }
            setter.accept(parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a positive integer.",
                    exception);
        }
    }

    private static void applyNonNegativeInt(
            Map<String, String> environment,
            String name,
            IntConsumer setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new IllegalArgumentException(
                        name + " must not be negative.");
            }
            setter.accept(parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a non-negative integer.",
                    exception);
        }
    }

    private static void applyPositiveLong(
            Map<String, String> environment,
            String name,
            LongConsumer setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(
                        name + " must be greater than zero.");
            }
            setter.accept(parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a positive integer.",
                    exception);
        }
    }

    private static void applyBoolean(
            Map<String, String> environment,
            String name,
            Consumer<Boolean> setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            throw new IllegalArgumentException(
                    name + " must be true or false.");
        }
        setter.accept(Boolean.parseBoolean(normalized));
    }

    private static void applyExecutionProvider(
            Map<String, String> environment,
            String name,
            Consumer<OnnxExecutionProvider> setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            setter.accept(OnnxExecutionProvider.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    name + " must be AUTO, CPU, or CUDA.",
                    exception);
        }
    }

    private static void applyDuration(
            Map<String, String> environment,
            String name,
            Consumer<Duration> setter) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return;
        }
        setter.accept(parseDuration(name, value.trim()));
    }

    private static Duration parseDuration(String name, String value) {
        if (value.startsWith("P") || value.startsWith("p")) {
            try {
                return Duration.parse(value.toUpperCase(Locale.ROOT));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(
                        name + " is not a valid duration.",
                        exception);
            }
        }

        Matcher matcher = SIMPLE_DURATION.matcher(
                value.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    name + " must use ms, s, m, h, or ISO-8601 format.");
        }

        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> throw new IllegalStateException(
                    "Unsupported duration unit.");
        };
    }
}
