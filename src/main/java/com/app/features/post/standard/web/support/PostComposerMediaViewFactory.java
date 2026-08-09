package com.app.features.post.standard.web.support;

import org.springframework.stereotype.Component;

import com.app.features.media.enums.MediaKind;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.post.standard.web.view.PostComposerMediaItemView;

@Component
public class PostComposerMediaViewFactory {

    public PostComposerMediaItemView toItem(MediaResult media) {
        return PostComposerMediaItemView.builder()
                .id(media.getId())
                .originalName(media.getOriginalName())
                .kind(media.getKind())
                .previewUrl(resolvePreviewUrl(media))
                .iconClass(resolveIconClass(media.getKind()))
                .build();
    }

    private String resolvePreviewUrl(MediaResult media) {
        if (media.getThumbnailUrl() != null) {
            return media.getThumbnailUrl();
        }
        return media.getKind() == MediaKind.IMAGE
                ? media.getContentUrl()
                : null;
    }

    private String resolveIconClass(MediaKind kind) {
        return switch (kind) {
            case IMAGE -> "bi-image";
            case VIDEO -> "bi-camera-video";
            case AUDIO -> "bi-file-music";
            case DOCUMENT -> "bi-file-earmark-text";
            case FILE -> "bi-file-earmark";
        };
    }
}
