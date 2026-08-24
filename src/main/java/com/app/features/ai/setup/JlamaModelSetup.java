package com.app.features.ai.setup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.github.tjake.jlama.util.Downloader;

public final class JlamaModelSetup {

    private static final String MODEL_ID_ENV =
            "POST_AI_MODERATION_MODEL_ID";
    private static final String MODEL_DIRECTORY_ENV =
            "POST_AI_MODERATION_MODEL_DIRECTORY";
    private static final String DEFAULT_MODEL_ID =
            "tjake/Qwen2.5-0.5B-Instruct-JQ4";
    private static final String DEFAULT_MODEL_DIRECTORY =
            "ai-models/jlama";

    private JlamaModelSetup() {
    }

    public static void main(String[] args) throws IOException {
        String modelId = resolveValue(
                args,
                0,
                MODEL_ID_ENV,
                DEFAULT_MODEL_ID);
        Path modelDirectory = Path.of(resolveValue(
                        args,
                        1,
                        MODEL_DIRECTORY_ENV,
                        DEFAULT_MODEL_DIRECTORY))
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(modelDirectory);
        System.out.printf(
                "Downloading Jlama model [%s] into [%s].%n",
                modelId,
                modelDirectory);

        File downloadedModel = new Downloader(
                modelDirectory.toString(),
                modelId)
                .huggingFaceModel();
        Path downloadedModelPath = downloadedModel.toPath()
                .toAbsolutePath()
                .normalize();

        validateModel(downloadedModelPath);
        System.out.printf(
                "Jlama model is ready at [%s].%n",
                downloadedModelPath);
    }

    private static String resolveValue(
            String[] args,
            int argumentIndex,
            String environmentName,
            String defaultValue) {
        if (args.length > argumentIndex
                && !args[argumentIndex].isBlank()) {
            return args[argumentIndex].trim();
        }

        String environmentValue = System.getenv(environmentName);
        return environmentValue == null || environmentValue.isBlank()
                ? defaultValue
                : environmentValue.trim();
    }

    private static void validateModel(Path modelPath) throws IOException {
        if (!Files.isDirectory(modelPath)) {
            throw new IOException(
                    "Downloaded model directory does not exist: " + modelPath);
        }

        requireRegularFile(modelPath.resolve("config.json"));
        requireRegularFile(modelPath.resolve("tokenizer.json"));

        try (Stream<Path> modelFiles = Files.list(modelPath)) {
            boolean hasSafeTensorWeights = modelFiles
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.endsWith(".safetensors"));
            if (!hasSafeTensorWeights) {
                throw new IOException(
                        "No SafeTensors weights were found in: " + modelPath);
            }
        }
    }

    private static void requireRegularFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Required model file is missing: " + path);
        }
    }
}
