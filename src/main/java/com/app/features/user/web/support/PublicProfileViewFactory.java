package com.app.features.user.web.support;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.features.user.service.ProfileService;
import com.app.features.user.web.enums.PublicProfileContentType;
import com.app.features.user.web.view.PublicProfileHeaderView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PublicProfileViewFactory {

    private final AppProperties appProperties;
    private final ProfileService profileSvc;

    public PublicProfileHeaderView build(
            UUID userId,
            PublicProfileContentType activeContentType) {
        if (userId == null) {
            return null;
        }

        return PublicProfileHeaderView.builder()
                .profile(profileSvc.getPublicProfile(userId))
                .activeContentType(activeContentType)
                .postsPath(buildContentPath(
                        appProperties.getUi().getFeedPath(),
                        userId))
                .shortsPath(buildContentPath(
                        appProperties.getUi().getShortsPath(),
                        userId))
                .videosPath(buildContentPath(
                        appProperties.getUi().getVideosPath(),
                        userId))
                .build();
    }

    public String buildProfilePath(UUID userId) {
        return buildContentPath(
                appProperties.getUi().getFeedPath(),
                userId);
    }

    private String buildContentPath(String path, UUID userId) {
        return UriComponentsBuilder.fromPath(path)
                .queryParam("authorId", userId)
                .build()
                .encode()
                .toUriString();
    }
}
