package com.app.features.media.validation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.core.exception.MyException;
import com.app.features.media.exception.InvalidMediaContentException;
import com.app.features.media.storage.schema.StagedMediaFile;
import com.app.features.media.validation.schema.ValidatedMediaFile;
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

    public ValidatedMediaFile validate(
            StagedMediaFile stagedFile,
            AllowedMediaType policy) {
        Path file = stagedFile.getTemporaryPath();
        String detectedContentType = mediaTypePolicyResolver.validateContentType(
                policy,
                detectContentType(file, stagedFile.getOriginalName()));

        return switch (policy.getKind()) {
            case IMAGE -> validateImage(file, detectedContentType);
            case VIDEO -> validateAudioVideo(
                    file,
                    detectedContentType,
                    StreamType.VIDEO);
            case AUDIO -> validateAudioVideo(
                    file,
                    detectedContentType,
                    StreamType.AUDIO);
            case DOCUMENT, FILE -> new ValidatedMediaFile(
                    detectedContentType,
                    null,
                    null,
                    null);
        };
    }

    private String detectContentType(Path file, String originalName) {
        try (InputStream input = Files.newInputStream(file)) {
            return tika.detect(input, originalName);
        } catch (IOException ex) {
            throw new InvalidMediaContentException(
                    "error.media.contentTypeUndetected");
        }
    }

    private ValidatedMediaFile validateImage(
            Path file,
            String contentType) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                throw new InvalidMediaContentException("error.media.imageFileInvalid");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidMediaContentException(
                        "error.media.imageUnsupported");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = Math.multiplyExact(
                        (long) width,
                        height);
                if (pixels > appProperties.getMedia().getMaxImagePixels()) {
                    throw new InvalidMediaContentException(
                            "error.media.imagePixelLimitExceeded");
                }

                BufferedImage decodedImage = reader.read(0);
                if (decodedImage == null) {
                    throw new InvalidMediaContentException(
                            "error.media.imageContentInvalid");
                }

                return new ValidatedMediaFile(
                        contentType,
                        width,
                        height,
                        null);
            } finally {
                reader.dispose();
            }
        } catch (ArithmeticException ex) {
            throw new InvalidMediaContentException(
                    "error.media.imageDimensionsInvalid");
        } catch (IOException ex) {
            throw new InvalidMediaContentException("error.media.imageContentInvalid");
        }
    }

    private ValidatedMediaFile validateAudioVideo(
            Path file,
            String contentType,
            StreamType requiredStreamType) {
        FFprobeResult result;
        try {
            result = mediaProbe.probe(file);
        } catch (MyException ex) {
            if (ex.getHttpStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                throw ex;
            }
            throw new InvalidMediaContentException(
                    ex.getMessageKey(),
                    ex.getMessageArguments().toArray());
        }
        List<Stream> streams = result.getStreams() == null
                ? List.of()
                : result.getStreams();
        boolean requiredStreamPresent = streams.stream()
                .anyMatch(stream -> requiredStreamType.equals(stream.getCodecType()));

        double duration = resolveDuration(result, streams);
        if (!requiredStreamPresent || duration <= 0) {
            throw new InvalidMediaContentException(
                    requiredStreamType == StreamType.VIDEO
                            ? "error.media.videoContentInvalid"
                            : "error.media.audioContentInvalid");
        }

        Stream videoStream = requiredStreamType == StreamType.VIDEO
                ? streams.stream()
                        .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                        .filter(stream -> stream.getWidth() != null
                                && stream.getWidth() > 0
                                && stream.getHeight() != null
                                && stream.getHeight() > 0)
                        .max((left, right) -> Integer.compare(
                                left.getHeight(),
                                right.getHeight()))
                        .orElse(null)
                : null;

        return new ValidatedMediaFile(
                contentType,
                videoStream == null ? null : videoStream.getWidth(),
                videoStream == null ? null : videoStream.getHeight(),
                Math.max(1L, Math.round(duration * 1_000)));
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
