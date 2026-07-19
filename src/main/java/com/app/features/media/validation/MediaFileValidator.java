package com.app.features.media.validation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.ExceptionFactory;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaFileValidator {

    private final AppProperties appProperties;
    private final MediaTypePolicyResolver mediaTypePolicyResolver;
    private final MediaProbe mediaProbe;

    private final Tika tika = new Tika();

    public String validate(Path file, AllowedMediaType policy) {
        String detectedContentType = mediaTypePolicyResolver.validateContentType(
                policy,
                detectContentType(file));

        switch (policy.getKind()) {
            case IMAGE -> validateImage(file);
            case VIDEO -> validateAudioVideo(file, StreamType.VIDEO);
            case AUDIO -> validateAudioVideo(file, StreamType.AUDIO);
            case DOCUMENT, FILE -> {
                // Extension, size, and Tika content type are sufficient here.
            }
        }

        return detectedContentType;
    }

    private String detectContentType(Path file) {
        try {
            return tika.detect(file);
        } catch (IOException ex) {
            throw ExceptionFactory.invalidParam("Unable to detect media content type.");
        }
    }

    private void validateImage(Path file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                throw ExceptionFactory.invalidParam("Invalid image file.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw ExceptionFactory.invalidParam("Unsupported image content.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = Math.multiplyExact(
                        (long) reader.getWidth(0),
                        reader.getHeight(0));
                if (pixels > appProperties.getMedia().getMaxImagePixels()) {
                    throw ExceptionFactory.invalidParam("Image dimensions exceed the allowed pixel count.");
                }

                BufferedImage decodedImage = reader.read(0);
                if (decodedImage == null) {
                    throw ExceptionFactory.invalidParam("Invalid image content.");
                }
            } finally {
                reader.dispose();
            }
        } catch (ArithmeticException ex) {
            throw ExceptionFactory.invalidParam("Image dimensions are invalid.");
        } catch (IOException ex) {
            throw ExceptionFactory.invalidParam("Invalid image content.");
        }
    }

    private void validateAudioVideo(Path file, StreamType requiredStreamType) {
        FFprobeResult result = mediaProbe.probe(file);
        List<Stream> streams = result.getStreams() == null
                ? List.of()
                : result.getStreams();
        boolean requiredStreamPresent = streams.stream()
                .anyMatch(stream -> requiredStreamType.equals(stream.getCodecType()));

        if (!requiredStreamPresent || resolveDuration(result, streams) <= 0) {
            throw ExceptionFactory.invalidParam(
                    requiredStreamType == StreamType.VIDEO
                            ? "Invalid video content."
                            : "Invalid audio content.");
        }
    }

    private double resolveDuration(FFprobeResult result, List<Stream> streams) {
        Format format = result.getFormat();
        if (format != null && format.getDuration() != null && format.getDuration() > 0) {
            return format.getDuration();
        }

        return streams.stream()
                .map(stream -> stream.getDuration())
                .filter(duration -> duration != null)
                .mapToDouble(duration -> duration)
                .max()
                .orElse(0);
    }
}
