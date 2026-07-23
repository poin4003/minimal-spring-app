package com.app.features.media.api.v1.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.exception.ExceptionFactory;
import com.app.core.response.ApiResult;
import com.app.core.security.UserPrincipal;
import com.app.features.media.schema.payload.StartMediaUploadPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaUploadSessionResult;
import com.app.features.media.service.MediaUploadService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media/uploads")
public class MediaUploadApiController {

    private final MediaUploadService mediaUploadSvc;

    @RateLimited(RateLimitPolicy.MEDIA_UPLOAD_SESSION)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<MediaUploadSessionResult> startUpload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody StartMediaUploadPayload payload) {
        MediaUploadSessionResult result = mediaUploadSvc.startUpload(
                currentUser.getUserId(),
                payload);
        return ApiResult.ok(result, "Media upload session created successfully.");
    }

    @RateLimited(RateLimitPolicy.MEDIA_UPLOAD_SESSION)
    @GetMapping("/{uploadId}")
    public ApiResult<MediaUploadSessionResult> getUpload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID uploadId) {
        MediaUploadSessionResult result = mediaUploadSvc.getUpload(
                uploadId,
                currentUser.getUserId());
        return ApiResult.ok(result, "Media upload session retrieved successfully.");
    }

    @RateLimited(RateLimitPolicy.MEDIA_UPLOAD_CHUNK)
    @PutMapping(
            path = "/{uploadId}/chunks/{chunkIndex}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ApiResult<Void> uploadChunk(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID uploadId,
            @PathVariable int chunkIndex,
            @RequestHeader(HttpHeaders.CONTENT_LENGTH) long contentLength,
            @RequestHeader("X-Chunk-SHA256") String checksum,
            HttpServletRequest request) {
        try {
            mediaUploadSvc.uploadChunk(
                    uploadId,
                    currentUser.getUserId(),
                    chunkIndex,
                    contentLength,
                    checksum,
                    request.getInputStream());
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to read media chunk request.");
        }
        return ApiResult.ok(null, "Media chunk uploaded successfully.");
    }

    @RateLimited(RateLimitPolicy.MEDIA_UPLOAD_SESSION)
    @PostMapping("/{uploadId}/complete")
    public ApiResult<MediaResult> completeUpload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID uploadId) {
        MediaResult result = mediaUploadSvc.completeUpload(
                uploadId,
                currentUser.getUserId());
        return ApiResult.ok(result, "Media upload completed successfully.");
    }

    @RateLimited(RateLimitPolicy.MEDIA_UPLOAD_SESSION)
    @DeleteMapping("/{uploadId}")
    public ApiResult<Void> cancelUpload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID uploadId) {
        mediaUploadSvc.cancelUpload(uploadId, currentUser.getUserId());
        return ApiResult.ok(null, "Media upload cancelled successfully.");
    }
}
