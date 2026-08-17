package com.app.features.media.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFfmpegFactory {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");

    private final AppProperties appProperties;
    private volatile Path wrappedExecutableDirectory;

    public FFmpeg create() {
        Path configuredPath = Path.of(appProperties.getMedia().getFfmpeg().getExecutable());
        Path executableDirectory = Files.isDirectory(configuredPath)
                ? configuredPath
                : configuredPath.getParent();
        Path effectiveDirectory = resolveDriverWrappedDirectory(executableDirectory);

        FFmpeg ffmpeg = effectiveDirectory == null
                ? FFmpeg.atPath()
                : FFmpeg.atPath(effectiveDirectory);

        long timeoutMillis = TimeUnit.MINUTES.toMillis(
                appProperties.getMedia().getFfmpeg().getProcessTimeoutMinutes());
        return ffmpeg.setExecutorTimeoutMillis(Math.toIntExact(timeoutMillis));
    }

    private Path resolveDriverWrappedDirectory(Path executableDirectory) {
        String driverName = appProperties.getMedia().getFfmpeg()
                .getMachine().getLibvaDriverName();
        if (IS_WINDOWS
                || driverName == null
                || driverName.isBlank()
                || executableDirectory == null) {
            return executableDirectory;
        }

        Path cached = wrappedExecutableDirectory;
        if (cached != null) {
            return cached;
        }

        Path realBinary = executableDirectory.resolve("ffmpeg");
        try {
            Path wrapperDirectory = Files.createTempDirectory("media-ffmpeg-wrapper-");
            Path wrapper = wrapperDirectory.resolve("ffmpeg");
            Files.writeString(
                    wrapper,
                    "#!/bin/sh\n"
                            + "export LIBVA_DRIVER_NAME=" + driverName + "\n"
                            + "exec \"" + realBinary + "\" \"$@\"\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Files.setPosixFilePermissions(
                    wrapper,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ,
                            PosixFilePermission.GROUP_EXECUTE,
                            PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_EXECUTE));
            wrapper.toFile().deleteOnExit();
            wrappedExecutableDirectory = wrapperDirectory;
            return wrapperDirectory;
        } catch (IOException ex) {
            log.warn("Failed to create LIBVA driver wrapper, using raw FFmpeg executable", ex);
            return executableDirectory;
        }
    }
}
