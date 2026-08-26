package com.app.features.ai.benchmark;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.app.config.settings.AppProperties;
import com.app.features.ai.benchmark.schema.model.AiBenchmarkReport;
import com.app.features.ai.benchmark.schema.model.AiBenchmarkReport.CaseResult;
import com.app.features.ai.benchmark.schema.model.AiBenchmarkReport.RuntimeInfo;
import com.app.features.ai.benchmark.support.AiBenchmarkConfiguration;
import com.app.features.ai.benchmark.support.AiBenchmarkMeasurement;
import com.app.features.ai.benchmark.support.AiBenchmarkMeasurement.MeasuredCase;
import com.app.features.ai.embedding.integration.onnx.MultilingualE5OnnxRuntime;
import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.runtime.OnnxSessionResource;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionFactory;
import com.app.features.ai.runtime.JlamaRuntime;
import com.app.features.ai.schema.model.JlamaGenerationResult;
import com.app.features.ai.vision.support.ClipVisionModelContract;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import tools.jackson.databind.ObjectMapper;

public final class AiBenchmarkRunner {

    private static final int REPORT_SCHEMA_VERSION = 1;
    private static final String STATUS_READY = "READY";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String STATUS_FAILED = "FAILED";
    private static final float JLAMA_TEMPERATURE = 0.0f;
    private static final String EMBEDDING_QUERY =
            "Tìm bài viết hướng dẫn tối ưu video trên máy chủ nhỏ.";
    private static final String EMBEDDING_PASSAGE = """
            Video được xử lý thành nhiều rendition HLS để người xem có thể
            chuyển đổi chất lượng theo băng thông. Máy chủ sử dụng hàng đợi
            nền để việc encode không chặn request của người dùng.
            """;
    private static final String JLAMA_SYSTEM_PROMPT = """
            You are a concise benchmark assistant. Follow the user request and
            answer without markdown.
            """;
    private static final String JLAMA_USER_PROMPT = """
            In one short paragraph, explain why semantic search uses text
            embeddings.
            """;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiBenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        BenchmarkOptions options = BenchmarkOptions.parse(args);
        if (options.help()) {
            printUsage();
            return;
        }

        AiBenchmarkReport report = run(options);
        writeReport(options.output(), report);
        System.out.printf(
                "AI benchmark [%s] finished with status [%s]; report: [%s].%n",
                report.capability(),
                report.status(),
                options.output());
    }

    private static AiBenchmarkReport run(BenchmarkOptions options) {
        AppProperties properties = AiBenchmarkConfiguration.fromEnvironment();
        return switch (options.capability()) {
            case "embedding" -> benchmarkEmbedding(properties, options);
            case "vision" -> benchmarkVision(properties, options);
            case "jlama" -> benchmarkJlama(properties, options);
            default -> throw new IllegalArgumentException(
                    "Unsupported AI benchmark capability: "
                            + options.capability());
        };
    }

    private static AiBenchmarkReport benchmarkEmbedding(
            AppProperties properties,
            BenchmarkOptions options) {
        AppProperties.EmbeddingSettings settings = properties.getAi()
                .getEmbedding();
        AppProperties.EmbeddingMachine machine = settings.getMachine();
        MultilingualE5OnnxRuntime runtime =
                new MultilingualE5OnnxRuntime(properties);
        double loadDurationMs = 0.0;
        try {
            long loadStart = System.nanoTime();
            runtime.start();
            loadDurationMs = elapsedMilliseconds(loadStart);
            if (!runtime.isReady()) {
                return report(
                        "embedding",
                        STATUS_UNAVAILABLE,
                        runtime.getModelVersion(),
                        loadDurationMs,
                        embeddingConfiguration(properties),
                        List.of(),
                        "Embedding runtime is unavailable; inspect the Java log.");
            }

            List<CaseResult> cases = new ArrayList<>();
            cases.add(AiBenchmarkMeasurement.measure(
                    "embedding-query-end-to-end",
                    options.warmupIterations(),
                    options.measuredIterations(),
                    () -> vectorMetrics(runtime.embedQuery(EMBEDDING_QUERY)))
                    .report());
            cases.add(AiBenchmarkMeasurement.measure(
                    "embedding-passage-end-to-end",
                    options.warmupIterations(),
                    options.measuredIterations(),
                    () -> vectorMetrics(runtime.embedPassage(
                            EMBEDDING_PASSAGE)))
                    .report());

            Map<String, Object> configuration =
                    embeddingConfiguration(properties);
            configuration.put("dimension", runtime.getDimension());
            configuration.put(
                    "activeExecutionProvider",
                    runtime.getRuntimeProvider());
            configuration.put(
                    "measurementScope",
                    "DJL tokenizer, ONNX inference, mean pooling and L2 normalization");
            return report(
                    "embedding",
                    STATUS_READY,
                    runtime.getModelVersion(),
                    loadDurationMs,
                    configuration,
                    cases,
                    null);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
            return report(
                    "embedding",
                    STATUS_FAILED,
                    settings.getModel().getId()
                            + "@"
                            + settings.getModel().getRevision(),
                    loadDurationMs,
                    embeddingConfiguration(properties),
                    List.of(),
                    describe(exception));
        } finally {
            runtime.close();
        }
    }

    private static AiBenchmarkReport benchmarkVision(
            AppProperties properties,
            BenchmarkOptions options) {
        AppProperties.VisionSettings settings = properties.getAi().getVision();
        AppProperties.VisionModel model = settings.getModel();
        AppProperties.VisionMachine machine = settings.getMachine();
        OnnxModelArtifact artifact = new OnnxModelArtifact(
                model.getId(),
                model.getRevision(),
                model.getSha256(),
                model.getFile());
        OnnxSessionResource sessionResource = null;
        double loadDurationMs = 0.0;
        try {
            Path modelPath = OnnxModelFiles.resolveModelPath(
                    machine.getModelDirectory(),
                    artifact);
            if (!Files.isRegularFile(modelPath)) {
                throw new IllegalStateException(
                        "CLIP ONNX model is missing at: " + modelPath);
            }

            long loadStart = System.nanoTime();
            OnnxModelFiles.verifySha256(modelPath, artifact.sha256());
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            sessionResource = OnnxSessionFactory.create(
                    environment,
                    modelPath.toString(),
                    machine,
                    properties.getAi().getOnnx());
            ClipVisionModelContract.validateAndRunSmokeInference(
                    environment,
                    sessionResource.session());
            loadDurationMs = elapsedMilliseconds(loadStart);

            OrtSession benchmarkSession = sessionResource.session();
            CaseResult smokeInference = AiBenchmarkMeasurement.measure(
                    "clip-onnx-smoke-inference",
                    options.warmupIterations(),
                    options.measuredIterations(),
                    () -> {
                        ClipVisionModelContract.validateAndRunSmokeInference(
                                environment,
                                benchmarkSession);
                        return Map.of();
                    }).report();
            Map<String, Object> configuration = visionConfiguration(properties);
            configuration.put(
                    "activeExecutionProvider",
                    sessionResource.executionProvider().name());
            configuration.put(
                    "measurementScope",
                    "ONNX inference with correctly shaped synthetic tensors; image preprocessing excluded");
            return report(
                    "vision",
                    STATUS_READY,
                    artifact.id() + "@" + artifact.revision(),
                    loadDurationMs,
                    configuration,
                    List.of(smokeInference),
                    null);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
            return report(
                    "vision",
                    STATUS_FAILED,
                    artifact.id() + "@" + artifact.revision(),
                    loadDurationMs,
                    visionConfiguration(properties),
                    List.of(),
                    describe(exception));
        } finally {
            closeSession(sessionResource);
        }
    }

    private static AiBenchmarkReport benchmarkJlama(
            AppProperties properties,
            BenchmarkOptions options) {
        AppProperties.AiModerationMachine machine = properties.getPost()
                .getAiModeration()
                .getMachine();
        JlamaRuntime runtime = new JlamaRuntime(properties);
        double loadDurationMs = 0.0;
        try {
            long loadStart = System.nanoTime();
            runtime.start();
            loadDurationMs = elapsedMilliseconds(loadStart);
            if (!runtime.isReady()) {
                return report(
                        "jlama",
                        STATUS_UNAVAILABLE,
                        runtime.getModelId(),
                        loadDurationMs,
                        jlamaConfiguration(machine, options),
                        List.of(),
                        "Jlama runtime is unavailable; inspect the Java log.");
            }

            MeasuredCase measured = AiBenchmarkMeasurement.measure(
                    "jlama-generation-end-to-end",
                    options.warmupIterations(),
                    options.measuredIterations(),
                    () -> jlamaMetrics(runtime.generateWithMetrics(
                            JLAMA_SYSTEM_PROMPT,
                            JLAMA_USER_PROMPT,
                            JLAMA_TEMPERATURE,
                            options.jlamaMaxTokens())));
            double generatedTokens = measured.total("generatedTokens");
            double generationTimeMs = measured.total("generationTimeMs");
            Map<String, Double> derivedMetrics = new LinkedHashMap<>();
            derivedMetrics.put("generatedTokensTotal", generatedTokens);
            derivedMetrics.put(
                    "generatedTokensPerSecond",
                    generationTimeMs <= 0.0
                            ? 0.0
                            : generatedTokens / (generationTimeMs / 1_000.0));
            CaseResult generation = AiBenchmarkMeasurement.addMetrics(
                    measured.report(),
                    derivedMetrics);
            return report(
                    "jlama",
                    STATUS_READY,
                    runtime.getModelId(),
                    loadDurationMs,
                    jlamaConfiguration(machine, options),
                    List.of(generation),
                    null);
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
            return report(
                    "jlama",
                    STATUS_FAILED,
                    machine.getModelId(),
                    loadDurationMs,
                    jlamaConfiguration(machine, options),
                    List.of(),
                    describe(exception));
        } finally {
            runtime.close();
        }
    }

    private static Map<String, Double> vectorMetrics(float[] vector) {
        double squaredNorm = 0.0;
        for (float value : vector) {
            squaredNorm += value * value;
        }
        return Map.of(
                "vectorDimension",
                (double) vector.length,
                "vectorNorm",
                Math.sqrt(squaredNorm));
    }

    private static Map<String, Double> jlamaMetrics(
            JlamaGenerationResult result) {
        return Map.of(
                "promptTokens",
                (double) result.promptTokens(),
                "generatedTokens",
                (double) result.generatedTokens(),
                "promptTimeMs",
                (double) result.promptTimeMs(),
                "generationTimeMs",
                (double) result.generationTimeMs(),
                "responseCharacters",
                (double) result.responseText().length());
    }

    private static Map<String, Object> embeddingConfiguration(
            AppProperties properties) {
        AppProperties.EmbeddingSettings settings = properties.getAi()
                .getEmbedding();
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("threads", settings.getMachine().getThreads());
        configuration.put(
                "maxConcurrency",
                settings.getMachine().getMaxConcurrency());
        configuration.put(
                "modelDirectory",
                settings.getMachine().getModelDirectory());
        addOnnxConfiguration(
                configuration,
                settings.getMachine(),
                properties.getAi().getOnnx());
        return configuration;
    }

    private static Map<String, Object> visionConfiguration(
            AppProperties properties) {
        AppProperties.VisionSettings settings = properties.getAi().getVision();
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("threads", settings.getMachine().getThreads());
        configuration.put(
                "maxConcurrency",
                settings.getMachine().getMaxConcurrency());
        configuration.put(
                "modelDirectory",
                settings.getMachine().getModelDirectory());
        addOnnxConfiguration(
                configuration,
                settings.getMachine(),
                properties.getAi().getOnnx());
        return configuration;
    }

    private static void addOnnxConfiguration(
            Map<String, Object> configuration,
            AppProperties.OnnxMachine machine,
            AppProperties.OnnxSettings onnxSettings) {
        configuration.put(
                "requestedExecutionProvider",
                machine.getExecutionProvider().name());
        configuration.put(
                "fallbackToCpu",
                onnxSettings.isFallbackToCpu());
        configuration.put(
                "cudaDeviceId",
                onnxSettings.getCuda().getDeviceId());
        configuration.put(
                "cudaMemoryLimitMb",
                onnxSettings.getCuda().getMemoryLimitMb());
    }

    private static Map<String, Object> jlamaConfiguration(
            AppProperties.AiModerationMachine machine,
            BenchmarkOptions options) {
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("threads", machine.getThreads());
        configuration.put("maxConcurrency", machine.getMaxConcurrency());
        configuration.put("modelDirectory", machine.getModelDirectory());
        configuration.put("maxOutputTokens", options.jlamaMaxTokens());
        configuration.put("temperature", JLAMA_TEMPERATURE);
        return configuration;
    }

    private static AiBenchmarkReport report(
            String capability,
            String status,
            String modelVersion,
            double loadDurationMs,
            Map<String, Object> configuration,
            List<CaseResult> cases,
            String error) {
        return new AiBenchmarkReport(
                REPORT_SCHEMA_VERSION,
                Instant.now().toString(),
                capability,
                status,
                modelVersion,
                loadDurationMs,
                runtimeInfo(),
                configuration,
                cases,
                error);
    }

    private static RuntimeInfo runtimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        return new RuntimeInfo(
                resolveHostname(),
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                runtime.availableProcessors(),
                runtime.maxMemory(),
                ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    private static String resolveHostname() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.isBlank()) {
            hostname = System.getenv("COMPUTERNAME");
        }
        return hostname == null || hostname.isBlank()
                ? "unknown"
                : hostname.trim();
    }

    private static double elapsedMilliseconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static String describe(Throwable exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getName()
                + (message == null || message.isBlank()
                        ? ""
                        : ": " + message);
    }

    private static void closeSession(OnnxSessionResource sessionResource) {
        if (sessionResource == null) {
            return;
        }
        try {
            sessionResource.close();
        } catch (Exception exception) {
            System.err.println(
                    "Unable to close benchmark ONNX session: "
                            + exception.getMessage());
        }
    }

    private static void writeReport(
            Path output,
            AiBenchmarkReport report) throws IOException {
        Files.createDirectories(output.getParent());
        String json = OBJECT_MAPPER
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(report);
        Files.writeString(
                output,
                json + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static void printUsage() {
        System.out.println("""
                AI benchmark runner

                Required environment variables:
                  AI_BENCHMARK_CAPABILITY=embedding|vision|jlama
                  AI_BENCHMARK_OUTPUT=path/to/report.json

                Optional environment variables:
                  AI_BENCHMARK_WARMUP=3
                  AI_BENCHMARK_ITERATIONS=20
                  AI_BENCHMARK_JLAMA_MAX_TOKENS=32

                Equivalent --key=value command-line arguments are supported.
                """);
    }

    private record BenchmarkOptions(
            String capability,
            Path output,
            int warmupIterations,
            int measuredIterations,
            int jlamaMaxTokens,
            boolean help) {

        private static BenchmarkOptions parse(String[] args) {
            Map<String, String> arguments = new LinkedHashMap<>();
            boolean help = false;
            for (String argument : args) {
                if ("--help".equals(argument) || "-h".equals(argument)) {
                    help = true;
                    continue;
                }
                if (!argument.startsWith("--") || !argument.contains("=")) {
                    throw new IllegalArgumentException(
                            "Benchmark arguments must use --key=value format.");
                }
                int separator = argument.indexOf('=');
                arguments.put(
                        argument.substring(2, separator),
                        argument.substring(separator + 1));
            }

            String capability = resolve(
                    arguments,
                    "capability",
                    "AI_BENCHMARK_CAPABILITY",
                    help ? "embedding" : null);
            int defaultWarmup = "jlama".equals(capability) ? 1 : 3;
            int defaultIterations = switch (capability) {
                case "jlama" -> 3;
                case "vision" -> 10;
                default -> 20;
            };
            String outputValue = resolve(
                    arguments,
                    "output",
                    "AI_BENCHMARK_OUTPUT",
                    help ? ".runtime/ai-benchmark-help.json" : null);
            return new BenchmarkOptions(
                    capability.toLowerCase(Locale.ROOT),
                    Path.of(outputValue).toAbsolutePath().normalize(),
                    positiveInt(resolve(
                            arguments,
                            "warmup",
                            "AI_BENCHMARK_WARMUP",
                            Integer.toString(defaultWarmup)),
                            "warmup"),
                    positiveInt(resolve(
                            arguments,
                            "iterations",
                            "AI_BENCHMARK_ITERATIONS",
                            Integer.toString(defaultIterations)),
                            "iterations"),
                    positiveInt(resolve(
                            arguments,
                            "max-tokens",
                            "AI_BENCHMARK_JLAMA_MAX_TOKENS",
                            "32"),
                            "max-tokens"),
                    help);
        }

        private static String resolve(
                Map<String, String> arguments,
                String argumentName,
                String environmentName,
                String defaultValue) {
            String value = arguments.get(argumentName);
            if (value == null || value.isBlank()) {
                value = System.getenv(environmentName);
            }
            if (value == null || value.isBlank()) {
                if (defaultValue == null) {
                    throw new IllegalArgumentException(
                            environmentName + " is required.");
                }
                return defaultValue;
            }
            return value.trim();
        }

        private static int positiveInt(String value, String name) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    throw new IllegalArgumentException(
                            name + " must be greater than zero.");
                }
                return parsed;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        name + " must be a positive integer.",
                        exception);
            }
        }
    }
}
