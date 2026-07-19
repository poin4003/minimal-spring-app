package com.app.features.media.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaProbe {

    private final AppProperties appProperties;

    public FFprobeResult probe(Path file) {
        Future<FFprobeResult> resultFuture = createProbe()
                .setShowStreams(true)
                .setShowFormat(true)
                .setInput(file)
                .executeAsync();

        try {
            return resultFuture.get(
                    appProperties.getMedia().getFfmpeg().getProcessTimeoutMinutes(),
                    TimeUnit.MINUTES);
        } catch (TimeoutException ex) {
            resultFuture.cancel(true);
            throw ExceptionFactory.invalidParam("Media probing timed out.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            resultFuture.cancel(true);
            throw ExceptionFactory.serverError("Media probing was interrupted.");
        } catch (ExecutionException ex) {
            throw ExceptionFactory.invalidParam("Unable to probe audio or video content.");
        }
    }

    private FFprobe createProbe() {
        Path configuredPath = Path.of(
                appProperties.getMedia().getFfmpeg().getFfprobeExecutable());
        if (Files.isDirectory(configuredPath)) {
            return FFprobe.atPath(configuredPath);
        }

        Path binaryDirectory = configuredPath.getParent();
        return binaryDirectory == null
                ? FFprobe.atPath()
                : FFprobe.atPath(binaryDirectory);
    }
}
