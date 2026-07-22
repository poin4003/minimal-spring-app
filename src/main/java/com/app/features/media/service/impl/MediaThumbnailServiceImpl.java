package com.app.features.media.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.schema.model.MediaThumbnailResult;
import com.app.features.media.service.MediaThumbnailService;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.schema.MediaThumbnailWorkspace;
import com.app.features.media.support.MediaFfmpegFactory;
import com.app.features.media.support.MediaProcessingPolicy;
import com.app.features.media.validation.MediaProbe;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaThumbnailServiceImpl implements MediaThumbnailService {

    private static final String JPEG_FORMAT = "jpg";

    private final MediaFileStorage mediaFileStorage;
    private final MediaProbe mediaProbe;
    private final MediaFfmpegFactory mediaFfmpegFactory;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final AppProperties appProperties;

    @Override
    public Optional<MediaThumbnailResult> generateThumbnail(MediaEntity media) {
        if (!mediaProcessingPolicy.shouldGenerateThumbnail(media)) {
            return Optional.empty();
        }

        Path source = mediaFileStorage.resolve(media.getStorageKey());
        MediaThumbnailWorkspace workspace = mediaFileStorage
                .prepareThumbnailWorkspace(media.getStorageKey());

        try {
            BufferedImage image = switch (media.getKind()) {
                case IMAGE -> extractVisualThumbnail(source, workspace, false);
                case VIDEO -> extractVisualThumbnail(source, workspace, true);
                case AUDIO -> extractAudioCover(source, workspace).orElse(null);
                case DOCUMENT -> renderPdfThumbnail(source);
                case FILE -> null;
            };

            if (image == null) {
                return Optional.empty();
            }

            BufferedImage normalizedImage = normalizeImage(image);
            writeJpeg(normalizedImage, workspace.getTemporaryFile());
            mediaFileStorage.publishThumbnailWorkspace(workspace);

            return Optional.of(new MediaThumbnailResult(
                    workspace.getPublishedStorageKey()));
        } finally {
            mediaFileStorage.discardThumbnailWorkspace(workspace);
        }
    }

    @Override
    public MediaThumbnailResult copyThumbnail(
            MediaEntity targetMedia,
            MediaEntity sourceMedia) {
        Path sourceThumbnail = mediaFileStorage.resolve(
                sourceMedia.getThumbnailStorageKey());
        if (!Files.isRegularFile(sourceThumbnail) || !Files.isReadable(sourceThumbnail)) {
            throw ExceptionFactory.invalidParam("Source media thumbnail is unavailable.");
        }

        MediaThumbnailWorkspace workspace = mediaFileStorage
                .prepareThumbnailWorkspace(targetMedia.getStorageKey());
        try {
            Files.copy(
                    sourceThumbnail,
                    workspace.getTemporaryFile(),
                    StandardCopyOption.REPLACE_EXISTING);
            readImage(workspace.getTemporaryFile());
            mediaFileStorage.publishThumbnailWorkspace(workspace);
            return new MediaThumbnailResult(workspace.getPublishedStorageKey());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to copy media thumbnail.");
        } finally {
            mediaFileStorage.discardThumbnailWorkspace(workspace);
        }
    }

    private BufferedImage extractVisualThumbnail(
            Path source,
            MediaThumbnailWorkspace workspace,
            boolean seekVideo) {
        Path extractedImage = workspace.getTemporaryDirectory().resolve("source.png");
        UrlInput input = UrlInput.fromPath(source);
        if (seekVideo) {
            input.setPosition(resolveVideoFramePositionMillis(source), TimeUnit.MILLISECONDS);
        }

        UrlOutput output = UrlOutput.toPath(extractedImage)
                .setCodec(StreamType.VIDEO, "png")
                .setFrameCount(StreamType.VIDEO, 1L)
                .disableStream(StreamType.AUDIO)
                .addArguments("-vf", thumbnailScaleFilter());

        mediaFfmpegFactory.create()
                .addInput(input)
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();

        return readImage(extractedImage);
    }

    private Optional<BufferedImage> extractAudioCover(
            Path source,
            MediaThumbnailWorkspace workspace) {
        FFprobeResult probeResult = mediaProbe.probe(source);
        boolean hasVisualStream = probeResult.getStreams().stream()
                .anyMatch(stream -> StreamType.VIDEO.equals(stream.getCodecType()));
        if (!hasVisualStream) {
            return Optional.empty();
        }

        Path extractedImage = workspace.getTemporaryDirectory().resolve("cover.png");
        UrlOutput output = UrlOutput.toPath(extractedImage)
                .addMap("0:v:0")
                .setCodec(StreamType.VIDEO, "png")
                .setFrameCount(StreamType.VIDEO, 1L)
                .disableStream(StreamType.AUDIO)
                .addArguments("-vf", thumbnailScaleFilter());

        mediaFfmpegFactory.create()
                .addInput(UrlInput.fromPath(source))
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();

        return Optional.of(readImage(extractedImage));
    }

    private BufferedImage renderPdfThumbnail(Path source) {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw ExceptionFactory.invalidParam("PDF document has no pages.");
            }

            PDRectangle cropBox = document.getPage(0).getCropBox();
            float pageWidth = cropBox.getWidth();
            float pageHeight = cropBox.getHeight();
            int rotation = Math.floorMod(document.getPage(0).getRotation(), 360);
            if (rotation == 90 || rotation == 270) {
                float previousWidth = pageWidth;
                pageWidth = pageHeight;
                pageHeight = previousWidth;
            }

            AppProperties.Thumbnail config = appProperties.getMedia().getThumbnail();
            float scale = Math.min(
                    config.getMaxWidth() / pageWidth,
                    config.getMaxHeight() / pageHeight);
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImage(0, scale, ImageType.RGB);
        } catch (IOException ex) {
            throw ExceptionFactory.invalidParam("Unable to render PDF thumbnail.");
        }
    }

    private long resolveVideoFramePositionMillis(Path source) {
        int configuredSecond = appProperties.getMedia()
                .getThumbnail()
                .getVideoFrameSecond();
        FFprobeResult probeResult = mediaProbe.probe(source);
        Float duration = probeResult.getFormat() == null
                ? null
                : probeResult.getFormat().getDuration();
        if (duration == null || duration <= 0) {
            return TimeUnit.SECONDS.toMillis(configuredSecond);
        }

        long durationMillis = Math.max(0L, Math.round(duration * 1_000));
        long configuredMillis = TimeUnit.SECONDS.toMillis(configuredSecond);
        return configuredMillis < durationMillis
                ? configuredMillis
                : durationMillis / 2;
    }

    private String thumbnailScaleFilter() {
        AppProperties.Thumbnail config = appProperties.getMedia().getThumbnail();
        return "scale="
                + config.getMaxWidth()
                + ":"
                + config.getMaxHeight()
                + ":force_original_aspect_ratio=decrease";
    }

    private BufferedImage normalizeImage(BufferedImage source) {
        BufferedImage normalized = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
    }

    private BufferedImage readImage(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw ExceptionFactory.invalidParam("Generated thumbnail is not a valid image.");
            }
            return image;
        } catch (IOException ex) {
            throw ExceptionFactory.invalidParam("Unable to read generated thumbnail.");
        }
    }

    private void writeJpeg(BufferedImage image, Path output) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);
        if (!writers.hasNext()) {
            throw ExceptionFactory.serverError("JPEG image writer is unavailable.");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(output.toFile())) {
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality((float) appProperties.getMedia()
                    .getThumbnail()
                    .getJpegQuality());
            writer.setOutput(outputStream);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to write media thumbnail.");
        } finally {
            writer.dispose();
        }
    }
}
