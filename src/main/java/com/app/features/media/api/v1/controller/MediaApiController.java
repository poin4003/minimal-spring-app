package com.app.features.media.api.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.response.ApiResult;
import com.app.core.security.UserPrincipal;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.payload.UpdateMediaThumbnailPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaApiController {

    private final MediaService mediaSvc;

    @RateLimited(RateLimitPolicy.MEDIA_DIRECT_UPLOAD)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<MediaResult> upload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @ModelAttribute CreateMediaPayload payload) {
        MediaResult result = mediaSvc.createMedia(
                currentUser.getUserId(),
                payload);

        return ApiResult.ok(result, "Media uploaded successfully.");
    }

    @PutMapping("/thumbnail")
    public ApiResult<MediaResult> updateThumbnail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateMediaThumbnailPayload payload) {
        MediaResult result = mediaSvc.updateOwnedThumbnail(
                payload.getMediaId(),
                currentUser.getUserId(),
                payload.getThumbnailMediaId());

        return ApiResult.ok(result, "Media thumbnail updated successfully.");
    }
}
