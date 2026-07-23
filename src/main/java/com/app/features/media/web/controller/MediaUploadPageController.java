package com.app.features.media.web.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.core.constant.PermissionConstants;
import com.app.core.security.UserPrincipal;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.web.view.MediaUploadResultView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/media/uploads")
public class MediaUploadPageController {

    private final MediaService mediaSvc;

    @PostMapping(path = "/direct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured(PermissionConstants.MEDIA_MANAGE)
    public String uploadDirect(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @ModelAttribute CreateMediaPayload payload,
            Model model) {
        MediaResult media = mediaSvc.createMedia(
                currentUser.getUserId(),
                payload);

        model.addAttribute(
                MediaUploadResultView.ATTRIBUTE,
                MediaUploadResultView.builder()
                        .media(media)
                        .message("Media uploaded successfully.")
                        .build());

        return "media/fragments/upload-result :: result (result=${result})";
    }
}
