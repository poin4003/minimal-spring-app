package com.app.features.media.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaFfmpegFactory {

    private final AppProperties appProperties;

    public FFmpeg create() {
        Path configuredPath = Path.of(appProperties.getMedia().getFfmpeg().getExecutable());
        FFmpeg ffmpeg;
        if (Files.isDirectory(configuredPath)) {
            ffmpeg = FFmpeg.atPath(configuredPath);
        } else {
            Path binaryDirectory = configuredPath.getParent();
            ffmpeg = binaryDirectory == null
                    ? FFmpeg.atPath()
                    : FFmpeg.atPath(binaryDirectory);
        }

        long timeoutMillis = TimeUnit.MINUTES.toMillis(
                appProperties.getMedia().getFfmpeg().getProcessTimeoutMinutes());
        return ffmpeg.setExecutorTimeoutMillis(Math.toIntExact(timeoutMillis));
    }
}
