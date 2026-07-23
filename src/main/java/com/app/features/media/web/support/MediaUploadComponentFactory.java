package com.app.features.media.web.support;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.AllowedMediaType;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.media.web.view.MediaUploadRuleView;
import com.app.features.media.web.view.MediaUploadTransportView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaUploadComponentFactory {

    private final AppProperties appProperties;

    public MediaUploadComponentView buildLibraryUpload() {
        return buildUpload(
                "media-library-upload",
                "Upload Media",
                "Upload Media",
                "Select files to add to the media library.",
                "Upload queued",
                true,
                appProperties.getMedia().getAllowedTypes());
    }

    public MediaUploadComponentView buildThumbnailUpload() {
        List<AllowedMediaType> imageTypes = appProperties.getMedia()
                .getAllowedTypes()
                .stream()
                .filter(rule -> rule.getKind() == MediaKind.IMAGE)
                .toList();

        return buildUpload(
                "media-thumbnail-upload",
                "Upload Thumbnail Image",
                "Upload Thumbnail Image",
                "Upload one image to the media library. Select it after processing reaches READY.",
                "Upload image",
                false,
                imageTypes);
    }

    private MediaUploadComponentView buildUpload(
            String id,
            String triggerLabel,
            String title,
            String description,
            String submitLabel,
            boolean multiple,
            List<AllowedMediaType> allowedTypes) {
        List<MediaUploadRuleView> rules = allowedTypes.stream()
                .sorted(Comparator.comparing(rule -> rule.getExtension()))
                .map(rule -> MediaUploadRuleView.builder()
                        .extension(rule.getExtension())
                        .kind(rule.getKind())
                        .maxFileSizeBytes(rule.getMaxFileSizeBytes())
                        .contentTypes(List.copyOf(rule.getContentTypes()))
                        .build())
                .toList();

        String accept = rules.stream()
                .map(rule -> "." + rule.getExtension())
                .collect(Collectors.joining(","));
        String uploadBasePath = appProperties.getUi().getHomePath() + "/media/uploads";
        AppProperties.ChunkUpload chunkUpload = appProperties.getMedia().getChunkUpload();
        MediaUploadTransportView transport = MediaUploadTransportView.builder()
                .directUploadPath(uploadBasePath + "/direct")
                .chunkUploadPath(uploadBasePath + "/chunks")
                .directUploadThresholdBytes(chunkUpload.getDirectUploadThresholdBytes())
                .parallelChunks(chunkUpload.getParallelChunks())
                .build();

        return MediaUploadComponentView.builder()
                .id(id)
                .triggerLabel(triggerLabel)
                .title(title)
                .description(description)
                .submitLabel(submitLabel)
                .transport(transport)
                .accept(accept)
                .multiple(multiple)
                .rules(rules)
                .build();
    }
}
