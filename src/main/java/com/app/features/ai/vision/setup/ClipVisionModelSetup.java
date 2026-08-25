package com.app.features.ai.vision.setup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionOptionsFactory;
import com.app.features.ai.vision.support.ClipVisionModelContract;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import tools.jackson.databind.ObjectMapper;

public final class ClipVisionModelSetup {

    private static final String MODEL_ID_ENV = "AI_VISION_MODEL_ID";
    private static final String MODEL_REVISION_ENV =
            "AI_VISION_MODEL_REVISION";
    private static final String MODEL_SHA256_ENV = "AI_VISION_MODEL_SHA256";
    private static final String MODEL_FILE_ENV = "AI_VISION_MODEL_FILE";
    private static final String MODEL_DIRECTORY_ENV =
            "AI_VISION_MODEL_DIRECTORY";
    private static final String THREADS_ENV = "AI_VISION_THREADS";
    private static final String HF_TOKEN_ENV = "HF_TOKEN";

    private static final String DEFAULT_MODEL_ID =
            "openai/clip-vit-base-patch32";
    private static final String DEFAULT_MODEL_REVISION =
            "12b36594d53414ecfba93c7200dbb7c7db3c900a";
    private static final String DEFAULT_MODEL_SHA256 =
            "57879bb1c23cdeb350d23569dd251ed4b740a96d747c529e94a2bb8040ac5d00";
    private static final String DEFAULT_MODEL_FILE = "onnx/model.onnx";
    private static final String DEFAULT_MODEL_DIRECTORY =
            "./data/ai-models/onnx";
    private static final int DEFAULT_THREADS = 4;
    private static final int MANIFEST_VERSION = 1;
    private static final String READY_MANIFEST_FILE = ".ready.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ClipVisionModelSetup() {
    }

    public static void main(String[] args) throws IOException, OrtException {
        String modelId = resolveValue(args, 0, MODEL_ID_ENV, DEFAULT_MODEL_ID);
        String modelRevision = resolveValue(
                args,
                1,
                MODEL_REVISION_ENV,
                DEFAULT_MODEL_REVISION);
        String modelSha256 = resolveValue(
                args,
                2,
                MODEL_SHA256_ENV,
                DEFAULT_MODEL_SHA256);
        String modelFile = resolveValue(
                args,
                3,
                MODEL_FILE_ENV,
                DEFAULT_MODEL_FILE);
        String modelDirectory = resolveValue(
                args,
                4,
                MODEL_DIRECTORY_ENV,
                DEFAULT_MODEL_DIRECTORY);
        int threads = resolvePositiveInt(
                args,
                5,
                THREADS_ENV,
                DEFAULT_THREADS);
        OnnxModelArtifact artifact = new OnnxModelArtifact(
                modelId,
                modelRevision,
                modelSha256,
                modelFile);
        Path modelPath = OnnxModelFiles.resolveModelPath(
                modelDirectory,
                artifact);
        Path revisionDirectory = OnnxModelFiles
                .resolveRevisionDirectory(
                        modelDirectory,
                        artifact);
        Path manifestPath = revisionDirectory.resolve(READY_MANIFEST_FILE);

        if (!Files.isRegularFile(modelPath)
                || !hasExpectedChecksum(modelPath, modelSha256)) {
            downloadModel(
                    artifact,
                    modelPath);
        } else {
            System.out.printf(
                    "CLIP ONNX model file already exists at [%s].%n",
                    modelPath);
        }

        ClipVisionModelContract.Schema schema = validateModel(
                modelPath,
                threads);
        writeReadyManifest(
                manifestPath,
                new ReadyManifest(
                        MANIFEST_VERSION,
                        artifact.id(),
                        artifact.revision(),
                        artifact.file(),
                        artifact.sha256(),
                        Files.size(modelPath),
                        schema.inputNames(),
                        schema.outputNames(),
                        OrtEnvironment.getEnvironment().getVersion(),
                        Instant.now().toString()));

        System.out.printf(
                "CLIP ONNX model is ready at [%s]; manifest written to [%s].%n",
                modelPath,
                manifestPath);
    }

    private static void downloadModel(
            OnnxModelArtifact artifact,
            Path modelPath) throws IOException {
        Files.createDirectories(modelPath.getParent());
        Path partialPath = modelPath.resolveSibling(
                modelPath.getFileName() + ".part");
        Files.deleteIfExists(partialPath);

        URI downloadUri = buildDownloadUri(
                artifact);
        System.out.printf(
                "Downloading CLIP ONNX model [%s@%s] into [%s].%n",
                artifact.id(),
                artifact.revision(),
                modelPath);

        try {
            download(downloadUri, partialPath);
            OnnxModelFiles.verifySha256(
                    partialPath,
                    artifact.sha256());
            moveIntoPlace(partialPath, modelPath);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(partialPath);
            throw exception;
        }
    }

    private static ClipVisionModelContract.Schema validateModel(
            Path modelPath,
            int threads) throws OrtException {
        System.out.printf(
                "Validating CLIP ONNX contract with [%d] threads.%n",
                threads);
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions options =
                OnnxSessionOptionsFactory.create(threads);
                OrtSession session = environment.createSession(
                        modelPath.toString(),
                        options)) {
            return ClipVisionModelContract.validateAndRunSmokeInference(
                    environment,
                    session);
        }
    }

    private static void writeReadyManifest(
            Path manifestPath,
            ReadyManifest manifest) throws IOException {
        Files.createDirectories(manifestPath.getParent());
        Path partialPath = manifestPath.resolveSibling(
                manifestPath.getFileName() + ".part");
        Files.deleteIfExists(partialPath);

        try {
            String json = OBJECT_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(manifest);
            Files.writeString(
                    partialPath,
                    json + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            moveIntoPlace(partialPath, manifestPath);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(partialPath);
            throw exception;
        }
    }

    private static boolean hasExpectedChecksum(
            Path modelPath,
            String expectedSha256) {
        try {
            OnnxModelFiles.verifySha256(modelPath, expectedSha256);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static void download(URI uri, Path target) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofHours(2))
                .header("User-Agent", "minimal-spring-app-ai-setup")
                .GET();
        String token = System.getenv(HF_TOKEN_ENV);
        if (token != null && !token.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + token.trim());
        }

        try {
            HttpResponse<Path> response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofFile(
                            target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Unable to download CLIP ONNX model; HTTP status: "
                                + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while downloading CLIP ONNX model.",
                    exception);
        }
    }

    private static URI buildDownloadUri(OnnxModelArtifact artifact) {
        OnnxModelFiles.validateArtifact(artifact);
        String normalizedModelFile = artifact.file().replace('\\', '/');
        if (normalizedModelFile.startsWith("/")
                || normalizedModelFile.contains("../")) {
            throw new IllegalArgumentException(
                    "AI vision model file must be a safe relative path.");
        }

        return URI.create("https://huggingface.co/"
                + artifact.id()
                + "/resolve/"
                + artifact.revision()
                + "/"
                + normalizedModelFile);
    }

    private static void moveIntoPlace(Path source, Path target)
            throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String resolveValue(
            String[] args,
            int argumentIndex,
            String environmentName,
            String defaultValue) {
        if (args.length > argumentIndex
                && args[argumentIndex] != null
                && !args[argumentIndex].isBlank()) {
            return args[argumentIndex].trim();
        }

        String environmentValue = System.getenv(environmentName);
        return environmentValue == null || environmentValue.isBlank()
                ? defaultValue
                : environmentValue.trim();
    }

    private static int resolvePositiveInt(
            String[] args,
            int argumentIndex,
            String environmentName,
            int defaultValue) {
        String configuredValue = resolveValue(
                args,
                argumentIndex,
                environmentName,
                Integer.toString(defaultValue));
        try {
            int parsedValue = Integer.parseInt(configuredValue);
            if (parsedValue <= 0) {
                throw new IllegalArgumentException(
                        environmentName + " must be greater than zero.");
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    environmentName + " must be a positive integer.",
                    exception);
        }
    }

    private record ReadyManifest(
            int manifestVersion,
            String modelId,
            String modelRevision,
            String modelFile,
            String sha256,
            long modelSizeBytes,
            List<String> inputNames,
            List<String> outputNames,
            String onnxRuntimeVersion,
            String verifiedAt) {
    }
}
