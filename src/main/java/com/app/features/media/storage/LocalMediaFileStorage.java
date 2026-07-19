package com.app.features.media.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.ExceptionFactory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMediaFileStorage implements MediaFileStorage {

    private static final DateTimeFormatter STORAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final AppProperties appProperties;

    private Path storageRoot;
    private Path stagingRoot;

    @PostConstruct
    void initialize() {
        storageRoot = Path.of(appProperties.getMedia().getStoragePath())
                .toAbsolutePath()
                .normalize();
        stagingRoot = storageRoot.resolve(".staging");

        try {
            Files.createDirectories(stagingRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize media storage.", ex);
        }
    }

    @Override
    public StagedMediaFile stage(MultipartFile file, AllowedMediaType policy) {
        if (file == null || file.isEmpty()) {
            throw ExceptionFactory.invalidParam("Media file is required.");
        }

        String originalName = MediaFilenameSupport.normalize(file.getOriginalFilename());
        String extension = MediaFilenameSupport.extensionOf(originalName);
        if (!policy.getExtension().equals(extension)) {
            throw ExceptionFactory.invalidParam("Media extension does not match its upload policy.");
        }

        long maximumSize = policy.getMaxFileSizeBytes();
        if (file.getSize() > maximumSize) {
            throw ExceptionFactory.invalidParam("Media file exceeds the allowed size.");
        }

        Path temporaryPath = createTemporaryFile();
        try {
            file.transferTo(temporaryPath);
            long fileSize = Files.size(temporaryPath);
            if (fileSize == 0) {
                throw ExceptionFactory.invalidParam("Media file must not be empty.");
            }
            if (fileSize > maximumSize) {
                throw ExceptionFactory.invalidParam("Media file exceeds the allowed size.");
            }

            return new StagedMediaFile(temporaryPath, originalName, extension, fileSize);
        } catch (RuntimeException ex) {
            deleteQuietly(temporaryPath);
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(temporaryPath);
            throw ExceptionFactory.serverError("Unable to stage media file.");
        }
    }

    @Override
    public StoredMediaFile commit(
            StagedMediaFile stagedFile,
            String detectedContentType) {
        String storageKey = STORAGE_DATE_FORMAT.format(LocalDate.now())
                + "/"
                + UUID.randomUUID()
                + "/original."
                + stagedFile.getExtension();
        Path targetPath = resolveStorageKey(storageKey);

        try {
            Files.createDirectories(targetPath.getParent());
            moveStagedFile(stagedFile.getTemporaryPath(), targetPath);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to commit media file.");
        }

        return new StoredMediaFile(
                storageKey,
                stagedFile.getOriginalName(),
                detectedContentType,
                stagedFile.getFileSize());
    }

    @Override
    public void discard(StagedMediaFile stagedFile) {
        if (stagedFile != null) {
            deleteQuietly(stagedFile.getTemporaryPath());
        }
    }

    @Override
    public MediaProcessingWorkspace prepareProcessingWorkspace(String sourceStorageKey) {
        Path source = resolveStorageKey(sourceStorageKey);
        Path mediaDirectory = source.getParent();
        Path temporaryDirectory = mediaDirectory.resolve(
                ".hls-processing-" + UUID.randomUUID());
        Path publishedDirectory = mediaDirectory.resolve("hls");

        try {
            Files.createDirectories(temporaryDirectory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError(
                    "Unable to prepare media processing workspace.");
        }

        return new MediaProcessingWorkspace(
                temporaryDirectory,
                publishedDirectory,
                resolveParentStorageKey(sourceStorageKey) + "/hls");
    }

    @Override
    public void publishProcessingWorkspace(MediaProcessingWorkspace workspace) {
        try {
            deleteRecursively(workspace.getPublishedDirectory());
            moveDirectory(
                    workspace.getTemporaryDirectory(),
                    workspace.getPublishedDirectory());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to publish processed media.");
        }
    }

    @Override
    public void discardProcessingWorkspace(MediaProcessingWorkspace workspace) {
        if (workspace == null) {
            return;
        }

        try {
            deleteRecursively(workspace.getTemporaryDirectory());
        } catch (IOException ex) {
            log.warn(
                    "Failed to discard media processing workspace [{}]",
                    workspace.getTemporaryDirectory(),
                    ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        Path targetPath = resolveStorageKey(storageKey);
        Path mediaDirectory = targetPath.getParent();
        try {
            deleteRecursively(mediaDirectory);
            deleteParentIfEmpty(mediaDirectory.getParent());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to delete media file.");
        }
    }

    @Override
    public Path resolve(String storageKey) {
        return resolveStorageKey(storageKey);
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolveStorageKey(storageKey));
    }

    @Override
    public boolean deleteHlsArtifacts(String sourceStorageKey) {
        Path source = resolveStorageKey(sourceStorageKey);
        Path hlsDirectory = source.getParent().resolve("hls");
        boolean existed = Files.exists(hlsDirectory);

        try {
            deleteRecursively(hlsDirectory);
            return existed;
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to delete HLS artifacts.");
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile(stagingRoot, "upload-", ".tmp");
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to create media staging file.");
        }
    }

    private void moveStagedFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private String resolveParentStorageKey(String storageKey) {
        int separatorIndex = storageKey.lastIndexOf('/');
        if (separatorIndex < 0) {
            throw ExceptionFactory.invalidParam("Media storage key is invalid.");
        }
        return storageKey.substring(0, separatorIndex);
    }

    private Path resolveStorageKey(String storageKey) {
        Path resolvedPath = storageRoot.resolve(storageKey).normalize();
        if (!resolvedPath.startsWith(storageRoot) || resolvedPath.startsWith(stagingRoot)) {
            throw ExceptionFactory.invalidParam("Media storage key is invalid.");
        }
        return resolvedPath;
    }

    private void deleteParentIfEmpty(Path directory) throws IOException {
        if (directory == null || directory.equals(storageRoot) || directory.equals(stagingRoot)) {
            return;
        }
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (var entries = Files.list(directory)) {
            if (entries.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        if (!directory.startsWith(storageRoot)
                || directory.equals(storageRoot)
                || directory.equals(stagingRoot)) {
            throw ExceptionFactory.invalidParam("Media directory is invalid.");
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
        } catch (IOException ex) {
            log.warn("Failed to discard staged media file [{}]", path, ex);
        }
    }
}
