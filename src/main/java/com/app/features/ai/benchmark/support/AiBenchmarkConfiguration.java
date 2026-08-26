package com.app.features.ai.benchmark.support;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.app.config.settings.AppProperties;

public final class AiBenchmarkConfiguration {

    private static final Pattern SIMPLE_DURATION =
            Pattern.compile("([0-9]+)(ms|s|m|h)");

    private AiBenchmarkConfiguration() {
    }

    public static AppProperties fromEnvironment() {
        AppProperties properties = new AppProperties();
        Map<String, String> environment = System.getenv();

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

        AppProperties.AiModerationMachine jlamaMachine = properties.getPost()
                .getAiModeration()
                .getMachine();
        apply(
                environment,
                "POST_AI_MODERATION_MODEL_ID",
                jlamaMachine::setModelId);
        apply(
                environment,
                "POST_AI_MODERATION_MODEL_DIRECTORY",
                jlamaMachine::setModelDirectory);
        applyPositiveInt(
                environment,
                "POST_AI_MODERATION_THREADS",
                jlamaMachine::setThreads);
        applyPositiveInt(
                environment,
                "POST_AI_MODERATION_MAX_CONCURRENCY",
                jlamaMachine::setMaxConcurrency);
        applyPositiveInt(
                environment,
                "POST_AI_MODERATION_MAX_TOKENS",
                jlamaMachine::setMaxTokens);
        applyDuration(
                environment,
                "POST_AI_MODERATION_TIMEOUT",
                jlamaMachine::setTimeout);
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
