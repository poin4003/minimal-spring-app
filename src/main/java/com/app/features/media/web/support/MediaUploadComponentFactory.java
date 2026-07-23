package com.app.features.media.web.support;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.media.web.view.MediaUploadRuleView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaUploadComponentFactory {

    private final AppProperties appProperties;

    public MediaUploadComponentView buildLibraryUpload() {
        List<MediaUploadRuleView> rules = appProperties.getMedia()
                .getAllowedTypes()
                .stream()
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

        return MediaUploadComponentView.builder()
                .id("media-library-upload")
                .title("Upload Media")
                .description("Select files to add to the media library.")
                .uploadPath(appProperties.getUi().getHomePath() + "/media/uploads/direct")
                .accept(accept)
                .multiple(true)
                .rules(rules)
                .build();
    }
}
