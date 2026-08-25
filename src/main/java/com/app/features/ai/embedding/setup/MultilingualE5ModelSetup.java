package com.app.features.ai.embedding.setup;

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

import com.app.features.ai.embedding.support.MultilingualE5ModelContract;
import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionOptionsFactory;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import tools.jackson.databind.ObjectMapper;

public final class MultilingualE5ModelSetup {

    private static final String MODEL_ID_ENV = "AI_EMBEDDING_MODEL_ID";
    private static final String MODEL_REVISION_ENV =
            "AI_EMBEDDING_MODEL_REVISION";
    private static final String MODEL_SHA256_ENV =
            "AI_EMBEDDING_MODEL_SHA256";
    private static final String MODEL_FILE_ENV = "AI_EMBEDDING_MODEL_FILE";
    private static final String TOKENIZER_SHA256_ENV =
            "AI_EMBEDDING_TOKENIZER_SHA256";
    private static final String TOKENIZER_FILE_ENV =
            "AI_EMBEDDING_TOKENIZER_FILE";
    private static final String MODEL_DIRECTORY_ENV =
            "AI_EMBEDDING_MODEL_DIRECTORY";
    private static final String THREADS_ENV = "AI_EMBEDDING_THREADS";
    private static final String HF_TOKEN_ENV = "HF_TOKEN";

    private static final String DEFAULT_MODEL_ID =
            "intfloat/multilingual-e5-small";
    private static final String DEFAULT_MODEL_REVISION =
            "03415a4be176a1620747c692ed433219fabc3def";
    private static final String DEFAULT_MODEL_SHA256 =
            "ca456c06b3a9505ddfd9131408916dd79290368331e7d76bb621f1cba6bc8665";
    private static final String DEFAULT_MODEL_FILE = "onnx/model.onnx";
    private static final String DEFAULT_TOKENIZER_SHA256 =
            "0b44a9d7b51c3c62626640cda0e2c2f70fdacdc25bbbd68038369d14ebdf4c39";
    private static final String DEFAULT_TOKENIZER_FILE =
            "onnx/tokenizer.json";
    private static final String DEFAULT_MODEL_DIRECTORY =
            "./data/ai-models/onnx";
    private static final int DEFAULT_THREADS = 4;
    private static final int MANIFEST_VERSION = 1;
    private static final String READY_MANIFEST_FILE = ".ready.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MultilingualE5ModelSetup() {
    }

    public static void main(String[] args) throws IOException, OrtException {
        String modelId = resolveValue(args, 0, MODEL_ID_ENV, DEFAULT_MODEL_ID);
        String modelRevision = resolveValue(
                args,
                1,
                MODEL_REVISION_ENV,
                DEFAULT_MODEL_REVISION);
        OnnxModelArtifact modelArtifact = new OnnxModelArtifact(
                modelId,
                modelRevision,
                resolveValue(
                        args,
                        2,
                        MODEL_SHA256_ENV,
                        DEFAULT_MODEL_SHA256),
                resolveValue(
                        args,
                        3,
                        MODEL_FILE_ENV,
                        DEFAULT_MODEL_FILE));
        OnnxModelArtifact tokenizerArtifact = new OnnxModelArtifact(
                modelId,
                modelRevision,
                resolveValue(
                        args,
                        4,
                        TOKENIZER_SHA256_ENV,
                        DEFAULT_TOKENIZER_SHA256),
                resolveValue(
                        args,
                        5,
                        TOKENIZER_FILE_ENV,
                        DEFAULT_TOKENIZER_FILE));
        String modelDirectory = resolveValue(
                args,
                6,
                MODEL_DIRECTORY_ENV,
                DEFAULT_MODEL_DIRECTORY);
        int threads = resolvePositiveInt(
                args,
                7,
                THREADS_ENV,
                DEFAULT_THREADS);

        Path modelPath = OnnxModelFiles.resolveModelPath(
                modelDirectory,
                modelArtifact);
        Path tokenizerPath = OnnxModelFiles.resolveModelPath(
                modelDirectory,
                tokenizerArtifact);
        Path revisionDirectory = OnnxModelFiles.resolveRevisionDirectory(
                modelDirectory,
                modelArtifact);
        Path manifestPath = revisionDirectory.resolve(READY_MANIFEST_FILE);

        ensureArtifact(modelArtifact, modelPath, "model");
        ensureArtifact(tokenizerArtifact, tokenizerPath, "tokenizer");

        ValidationResult validation = validateModel(
                modelPath,
                tokenizerPath,
                threads);
        MultilingualE5ModelContract.Schema schema = validation.schema();
        writeReadyManifest(
                manifestPath,
                new ReadyManifest(
                        MANIFEST_VERSION,
                        modelArtifact.id(),
                        modelArtifact.revision(),
                        modelArtifact.file(),
                        modelArtifact.sha256(),
                        Files.size(modelPath),
                        tokenizerArtifact.file(),
                        tokenizerArtifact.sha256(),
                        Files.size(tokenizerPath),
                        validation.tokenizerVersion(),
                        schema.inputNames(),
                        schema.outputNames(),
                        schema.embeddingDimension(),
                        OrtEnvironment.getEnvironment().getVersion(),
                        Instant.now().toString()));

        System.out.printf(
                "Multilingual E5 ONNX model is ready at [%s]; "
                        + "manifest written to [%s].%n",
                modelPath,
                manifestPath);
    }

    private static void ensureArtifact(
            OnnxModelArtifact artifact,
            Path artifactPath,
            String artifactType) throws IOException {
        if (!Files.isRegularFile(artifactPath)
                || !hasExpectedChecksum(artifactPath, artifact.sha256())) {
            downloadArtifact(artifact, artifactPath, artifactType);
            return;
        }

        System.out.printf(
                "Multilingual E5 %s already exists at [%s].%n",
                artifactType,
                artifactPath);
    }

    private static void downloadArtifact(
            OnnxModelArtifact artifact,
            Path artifactPath,
            String artifactType) throws IOException {
        Files.createDirectories(artifactPath.getParent());
        Path partialPath = artifactPath.resolveSibling(
                artifactPath.getFileName() + ".part");
        Files.deleteIfExists(partialPath);

        System.out.printf(
                "Downloading multilingual E5 %s [%s@%s] into [%s].%n",
                artifactType,
                artifact.id(),
                artifact.revision(),
                artifactPath);
        try {
            download(buildDownloadUri(artifact), partialPath, artifactType);
            OnnxModelFiles.verifySha256(
                    partialPath,
                    artifact.sha256());
            moveIntoPlace(partialPath, artifactPath);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(partialPath);
            throw exception;
        }
    }

    private static ValidationResult validateModel(
            Path modelPath,
            Path tokenizerPath,
            int threads) throws IOException, OrtException {
        System.out.printf(
                "Validating multilingual E5 ONNX contract with [%d] threads.%n",
                threads);
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (HuggingFaceTokenizer tokenizer =
                        MultilingualE5ModelContract.createTokenizer(tokenizerPath);
                OrtSession.SessionOptions options =
                        OnnxSessionOptionsFactory.create(threads);
                OrtSession session = environment.createSession(
                        modelPath.toString(),
                        options)) {
            MultilingualE5ModelContract.Schema schema =
                    MultilingualE5ModelContract
                            .validateAndRunSmokeInference(
                                    environment,
                                    session,
                                    tokenizer);
            return new ValidationResult(schema, tokenizer.getVersion());
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
            Path artifactPath,
            String expectedSha256) {
        try {
            OnnxModelFiles.verifySha256(
                    artifactPath,
                    expectedSha256);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static void download(
            URI uri,
            Path target,
            String artifactType) throws IOException {
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
                        "Unable to download multilingual E5 "
                                + artifactType
                                + "; HTTP status: "
                                + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while downloading multilingual E5 "
                            + artifactType
                            + ".",
                    exception);
        }
    }

    private static URI buildDownloadUri(OnnxModelArtifact artifact) {
        OnnxModelFiles.validateArtifact(artifact);
        String normalizedFile = artifact.file().replace('\\', '/');
        if (normalizedFile.startsWith("/")
                || normalizedFile.contains("../")) {
            throw new IllegalArgumentException(
                    "AI embedding artifact file must be a safe relative path.");
        }

        return URI.create("https://huggingface.co/"
                + artifact.id()
                + "/resolve/"
                + artifact.revision()
                + "/"
                + normalizedFile);
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

    private record ValidationResult(
            MultilingualE5ModelContract.Schema schema,
            String tokenizerVersion) {
    }

    private record ReadyManifest(
            int manifestVersion,
            String modelId,
            String modelRevision,
            String modelFile,
            String modelSha256,
            long modelSizeBytes,
            String tokenizerFile,
            String tokenizerSha256,
            long tokenizerSizeBytes,
            String tokenizerVersion,
            List<String> inputNames,
            List<String> outputNames,
            int embeddingDimension,
            String onnxRuntimeVersion,
            String verifiedAt) {
    }
}
