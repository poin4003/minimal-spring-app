package com.app.features.media.storage.impl;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.MediaFilenameSupport;
import com.app.features.media.storage.MediaStorageKeySupport;
import com.app.features.media.storage.schema.MediaProcessingWorkspace;
import com.app.features.media.storage.schema.MediaStorageDirectoryCandidate;
import com.app.features.media.storage.schema.MediaThumbnailWorkspace;
import com.app.features.media.storage.schema.StagedMediaFile;
import com.app.features.media.storage.schema.StoredMediaFile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMediaFileStorage implements MediaFileStorage {

    private static final DateTimeFormatter STORAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final List<String> PROCESSING_WORKSPACE_PREFIXES = List.of(
            ".hls-processing-",
            ".thumbnail-processing-");

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
            throw ExceptionFactory.serverError("Unable to stage media file.", ex);
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
            throw ExceptionFactory.serverError("Unable to commit media file.", ex);
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
                    "Unable to prepare media processing workspace.",
                    ex);
        }

        return new MediaProcessingWorkspace(
                temporaryDirectory,
                publishedDirectory,
                MediaStorageKeySupport.directoryOf(sourceStorageKey) + "/hls");
    }

    @Override
    public void publishProcessingWorkspace(MediaProcessingWorkspace workspace) {
        try {
            deleteRecursively(workspace.getPublishedDirectory());
            moveDirectory(
                    workspace.getTemporaryDirectory(),
                    workspace.getPublishedDirectory());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to publish processed media.", ex);
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
    public MediaThumbnailWorkspace prepareThumbnailWorkspace(String sourceStorageKey) {
        Path source = resolveStorageKey(sourceStorageKey);
        Path mediaDirectory = source.getParent();
        Path temporaryDirectory = mediaDirectory.resolve(
                ".thumbnail-processing-" + UUID.randomUUID());
        Path temporaryFile = temporaryDirectory.resolve("thumbnail.jpg");
        Path publishedFile = mediaDirectory.resolve("thumbnail.jpg");

        try {
            Files.createDirectories(temporaryDirectory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError(
                    "Unable to prepare thumbnail processing workspace.",
                    ex);
        }

        return new MediaThumbnailWorkspace(
                temporaryDirectory,
                temporaryFile,
                publishedFile,
                MediaStorageKeySupport.directoryOf(sourceStorageKey) + "/thumbnail.jpg");
    }

    @Override
    public void publishThumbnailWorkspace(MediaThumbnailWorkspace workspace) {
        if (!Files.isRegularFile(workspace.getTemporaryFile())) {
            throw ExceptionFactory.serverError("Generated thumbnail file is missing.");
        }

        try {
            moveFileReplacing(
                    workspace.getTemporaryFile(),
                    workspace.getPublishedFile());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to publish media thumbnail.", ex);
        }
    }

    @Override
    public void discardThumbnailWorkspace(MediaThumbnailWorkspace workspace) {
        if (workspace == null) {
            return;
        }

        try {
            deleteRecursively(workspace.getTemporaryDirectory());
        } catch (IOException ex) {
            log.warn(
                    "Failed to discard thumbnail workspace [{}]",
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
            throw ExceptionFactory.serverError("Unable to delete media file.", ex);
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
            throw ExceptionFactory.serverError("Unable to delete HLS artifacts.", ex);
        }
    }

    @Override
    public boolean deleteThumbnailArtifact(String sourceStorageKey) {
        Path source = resolveStorageKey(sourceStorageKey);
        Path thumbnail = source.getParent().resolve("thumbnail.jpg");
        try {
            return Files.deleteIfExists(thumbnail);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to delete media thumbnail.", ex);
        }
    }

    @Override
    public int deleteStagedFilesOlderThan(Instant cutoff, int limit) {
        List<Path> staleFiles;
        try (var paths = Files.list(stagingRoot)) {
            staleFiles = paths
                    .filter(path -> Files.isRegularFile(path))
                    .filter(path -> isOlderThan(path, cutoff))
                    .limit(limit)
                    .toList();
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to scan media staging files.", ex);
        }

        int deleted = 0;
        for (Path path : staleFiles) {
            if (deleteFileSafely(path)) {
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public int deleteProcessingWorkspacesOlderThan(Instant cutoff, int limit) {
        List<Path> staleDirectories;
        try (var paths = Files.walk(storageRoot, 4)) {
            staleDirectories = paths
                    .filter(path -> Files.isDirectory(path))
                    .filter(path -> isProcessingWorkspace(path.getFileName().toString()))
                    .filter(path -> isOlderThan(path, cutoff))
                    .limit(limit)
                    .toList();
        } catch (IOException ex) {
            throw ExceptionFactory.serverError(
                    "Unable to scan media processing workspaces.",
                    ex);
        }

        int deleted = 0;
        for (Path directory : staleDirectories) {
            if (deleteRecursivelySafely(directory)) {
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public List<MediaStorageDirectoryCandidate> findMediaDirectoriesOlderThan(
            Instant cutoff) {
        try (var paths = Files.walk(storageRoot, 3)) {
            return paths
                    .filter(path -> Files.isDirectory(path))
                    .filter(path -> isMediaDirectory(path))
                    .filter(path -> isOlderThan(path, cutoff))
                    .map(path -> new MediaStorageDirectoryCandidate(
                            toStorageKey(path)))
                    .toList();
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to scan media directories.", ex);
        }
    }

    @Override
    public boolean deleteMediaDirectory(String storageDirectoryKey) {
        Path directory = resolveStorageKey(storageDirectoryKey);
        if (!isMediaDirectory(directory)) {
            throw ExceptionFactory.invalidParam("Media directory key is invalid.");
        }

        boolean existed = Files.exists(directory);
        try {
            deleteRecursively(directory);
            deleteParentIfEmpty(directory.getParent());
            return existed;
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to delete media directory.", ex);
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile(stagingRoot, "upload-", ".tmp");
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to create media staging file.", ex);
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

    private void moveFileReplacing(Path source, Path target) throws IOException {
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

    private boolean isProcessingWorkspace(String fileName) {
        return PROCESSING_WORKSPACE_PREFIXES.stream()
                .anyMatch(prefix -> fileName.startsWith(prefix));
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

        boolean empty;
        try (var entries = Files.list(directory)) {
            empty = entries.findAny().isEmpty();
        }
        if (empty) {
            Files.deleteIfExists(directory);
            deleteParentIfEmpty(directory.getParent());
        }
    }

    private boolean isMediaDirectory(Path path) {
        if (!path.startsWith(storageRoot)) {
            return false;
        }

        Path relativePath = storageRoot.relativize(path);
        if (relativePath.getNameCount() != 3) {
            return false;
        }

        String year = relativePath.getName(0).toString();
        String month = relativePath.getName(1).toString();
        if (!year.matches("^[0-9]{4}$")
                || !month.matches("^(0[1-9]|1[0-2])$")) {
            return false;
        }

        try {
            UUID.fromString(relativePath.getName(2).toString());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String toStorageKey(Path path) {
        return storageRoot.relativize(path)
                .toString()
                .replace('\\', '/');
    }

    private boolean isOlderThan(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException ex) {
            log.warn("Unable to read media path timestamp [{}]", path, ex);
            return false;
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

    private boolean deleteFileSafely(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Failed to delete stale media file [{}]", path, ex);
            return false;
        }
    }

    private boolean deleteRecursivelySafely(Path directory) {
        try {
            boolean existed = Files.exists(directory);
            deleteRecursively(directory);
            return existed;
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to delete stale media directory [{}]", directory, ex);
            return false;
        }
    }
}
