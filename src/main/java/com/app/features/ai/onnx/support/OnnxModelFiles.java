package com.app.features.ai.onnx.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;

public final class OnnxModelFiles {

    private static final Pattern MODEL_COORDINATE_PATTERN =
            Pattern.compile("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+");
    private static final Pattern REVISION_PATTERN =
            Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SHA256_PATTERN =
            Pattern.compile("[0-9a-f]{64}");
    private static final int HASH_BUFFER_SIZE = 64 * 1024;

    private OnnxModelFiles() {
    }

    public static Path resolveModelPath(
            String modelDirectory,
            OnnxModelArtifact artifact) {
        validateArtifact(artifact);

        Path revisionRoot = resolveRevisionDirectory(
                modelDirectory,
                artifact);
        Path relativeModelFile = Path.of(artifact.file()).normalize();
        if (relativeModelFile.isAbsolute()
                || relativeModelFile.startsWith("..")) {
            throw new IllegalArgumentException(
                    "ONNX model file must be a safe relative path.");
        }

        Path modelPath = revisionRoot.resolve(relativeModelFile).normalize();
        if (!modelPath.startsWith(revisionRoot)) {
            throw new IllegalArgumentException(
                    "Resolved ONNX model path is outside its revision directory.");
        }

        return modelPath;
    }

    public static Path resolveRevisionDirectory(
            String modelDirectory,
            OnnxModelArtifact artifact) {
        validateArtifact(artifact);

        Path modelRoot = Path.of(modelDirectory)
                .toAbsolutePath()
                .normalize();
        String[] coordinates = artifact.id().split("/", -1);
        return modelRoot
                .resolve(coordinates[0])
                .resolve(coordinates[1])
                .resolve(artifact.revision())
                .normalize();
    }

    public static void verifySha256(
            Path modelPath,
            String expectedSha256) throws IOException {
        validateSha256(expectedSha256);

        String actualSha256 = calculateSha256(modelPath);
        if (!MessageDigest.isEqual(
                actualSha256.getBytes(StandardCharsets.US_ASCII),
                expectedSha256.getBytes(StandardCharsets.US_ASCII))) {
            throw new IOException(
                    "ONNX model checksum does not match: " + modelPath);
        }
    }

    public static String calculateSha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable in the current JVM.",
                    exception);
        }

        byte[] buffer = new byte[HASH_BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    public static void validateArtifact(OnnxModelArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException(
                    "ONNX model artifact is required.");
        }
        if (artifact.id() == null
                || !MODEL_COORDINATE_PATTERN.matcher(artifact.id()).matches()) {
            throw new IllegalArgumentException(
                    "ONNX model ID must use owner/name format.");
        }
        if (artifact.revision() == null
                || !REVISION_PATTERN.matcher(artifact.revision()).matches()) {
            throw new IllegalArgumentException(
                    "ONNX model revision must be a full commit hash.");
        }
        if (artifact.file() == null || artifact.file().isBlank()) {
            throw new IllegalArgumentException(
                    "ONNX model file is required.");
        }
        validateSha256(artifact.sha256());
    }

    private static void validateSha256(String sha256) {
        if (sha256 == null || !SHA256_PATTERN.matcher(sha256).matches()) {
            throw new IllegalArgumentException(
                    "ONNX model SHA-256 must be 64 lowercase hex characters.");
        }
    }
}
