package com.app.features.media.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.storage.MediaChunkStorage;
import com.app.features.media.storage.schema.StagedMediaFile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalMediaChunkStorage implements MediaChunkStorage {

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private final AppProperties appProperties;

    private Path uploadRoot;
    private Path stagingRoot;

    @PostConstruct
    void initialize() {
        Path storageRoot = Path.of(appProperties.getMedia().getStoragePath())
                .toAbsolutePath()
                .normalize();
        uploadRoot = storageRoot.resolve(".uploads");
        stagingRoot = storageRoot.resolve(".staging");

        try {
            Files.createDirectories(uploadRoot);
            Files.createDirectories(stagingRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize media chunk storage.", ex);
        }
    }

    @Override
    public void storeChunk(
            UUID uploadId,
            int chunkIndex,
            long expectedSize,
            String expectedChecksum,
            InputStream inputStream) {
        String normalizedChecksum = normalizeChecksum(expectedChecksum);
        Path uploadDirectory = resolveUploadDirectory(uploadId);
        Path target = resolveChunk(uploadId, chunkIndex);
        Path temporary = uploadDirectory.resolve(
                chunkFilename(chunkIndex) + ".tmp-" + UUID.randomUUID());

        try {
            Files.createDirectories(uploadDirectory);
            MessageDigest digest = sha256();
            long written = copyAndDigest(inputStream, temporary, expectedSize, digest);
            String actualChecksum = HexFormat.of().formatHex(digest.digest());
            if (written != expectedSize || !actualChecksum.equals(normalizedChecksum)) {
                throw ExceptionFactory.invalidParam("Media chunk size or checksum is invalid.");
            }

            moveAtomically(temporary, target);
        } catch (RuntimeException ex) {
            deleteQuietly(temporary);
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(temporary);
            throw ExceptionFactory.serverError("Unable to store media chunk.");
        }
    }

    @Override
    public List<Integer> findUploadedChunks(UUID uploadId) {
        Path uploadDirectory = resolveUploadDirectory(uploadId);
        if (!Files.isDirectory(uploadDirectory)) {
            return List.of();
        }

        try (var paths = Files.list(uploadDirectory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path))
                    .map(path -> parseChunkIndex(path.getFileName().toString()))
                    .filter(index -> index != null)
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to inspect uploaded media chunks.");
        }
    }

    @Override
    public StagedMediaFile assemble(
            UUID uploadId,
            String originalName,
            String extension,
            long expectedFileSize,
            int totalChunks) {
        Path assembledFile;
        try {
            assembledFile = Files.createTempFile(stagingRoot, "assembled-", ".tmp");
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to prepare assembled media file.");
        }

        try (OutputStream output = Files.newOutputStream(assembledFile)) {
            long assembledSize = 0;
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                Path chunk = resolveChunk(uploadId, chunkIndex);
                if (!Files.isRegularFile(chunk)) {
                    throw ExceptionFactory.invalidParam(
                            "Media upload is missing chunk: " + chunkIndex);
                }

                try (InputStream input = Files.newInputStream(chunk)) {
                    assembledSize += input.transferTo(output);
                }
            }

            if (assembledSize != expectedFileSize) {
                throw ExceptionFactory.invalidParam("Assembled media file size is invalid.");
            }

            return new StagedMediaFile(
                    assembledFile,
                    originalName,
                    extension,
                    assembledSize);
        } catch (RuntimeException ex) {
            deleteQuietly(assembledFile);
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(assembledFile);
            throw ExceptionFactory.serverError("Unable to assemble media chunks.");
        }
    }

    @Override
    public void deleteUpload(UUID uploadId) {
        Path uploadDirectory = resolveUploadDirectory(uploadId);
        try {
            deleteRecursively(uploadDirectory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to delete media upload chunks.");
        }
    }

    private long copyAndDigest(
            InputStream input,
            Path target,
            long expectedSize,
            MessageDigest digest) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long written = 0;

        try (OutputStream output = Files.newOutputStream(target)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                written += read;
                if (written > expectedSize) {
                    throw ExceptionFactory.invalidParam("Media chunk exceeds its expected size.");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        return written;
    }

    private Path resolveUploadDirectory(UUID uploadId) {
        if (uploadId == null) {
            throw ExceptionFactory.invalidParam("Media upload ID is required.");
        }
        return uploadRoot.resolve(uploadId.toString()).normalize();
    }

    private Path resolveChunk(UUID uploadId, int chunkIndex) {
        if (chunkIndex < 0) {
            throw ExceptionFactory.invalidParam("Media chunk index is invalid.");
        }
        return resolveUploadDirectory(uploadId).resolve(chunkFilename(chunkIndex));
    }

    private String chunkFilename(int chunkIndex) {
        return String.format("%08d.part", chunkIndex);
    }

    private Integer parseChunkIndex(String filename) {
        if (!filename.matches("^[0-9]{8}\\.part$")) {
            return null;
        }
        return Integer.valueOf(filename.substring(0, 8));
    }

    private String normalizeChecksum(String checksum) {
        if (checksum == null || !checksum.matches("^[a-fA-F0-9]{64}$")) {
            throw ExceptionFactory.invalidParam("Media chunk SHA-256 checksum is invalid.");
        }
        return checksum.toLowerCase(java.util.Locale.ROOT);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        if (!directory.startsWith(uploadRoot) || directory.equals(uploadRoot)) {
            throw ExceptionFactory.invalidParam("Media upload directory is invalid.");
        }

        List<Path> paths;
        try (var entries = Files.walk(directory)) {
            paths = entries
                    .sorted((left, right) -> right.compareTo(left))
                    .toList();
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The abandoned temporary file is handled by storage maintenance.
        }
    }
}
