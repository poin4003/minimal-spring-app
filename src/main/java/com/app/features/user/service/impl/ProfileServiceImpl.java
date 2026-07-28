package com.app.features.user.service.impl;

import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.service.MediaService;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.repository.UserInfoRepository;
import com.app.features.user.schema.payload.UpdateProfilePayload;
import com.app.features.user.schema.result.ProfileResult;
import com.app.features.user.service.ProfileService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserInfoRepository userInfoRepo;
    private final MediaService mediaSvc;
    private final ModelMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public ProfileResult getProfile(UUID userId) {
        return toResult(requireProfile(userId));
    }

    @Override
    @Transactional
    public ProfileResult updateProfile(
            UUID userId,
            UpdateProfilePayload payload) {
        UserInfoEntity profile = requireProfile(userId);
        mapper.map(payload, profile);
        return toResult(profile);
    }

    @Override
    @Transactional
    public ProfileResult updateTheme(
            UUID userId,
            boolean darkThemeEnabled) {
        UserInfoEntity profile = requireProfile(userId);
        profile.setDarkThemeEnabled(darkThemeEnabled);
        return toResult(profile);
    }

    @Override
    @Transactional
    public void updateAvatar(
            UUID userId,
            UUID avatarMediaId) {
        MediaEntity avatar = mediaSvc
                .requireOwnedActiveMedia(List.of(avatarMediaId), userId)
                .getFirst();

        if (avatar.getKind() != MediaKind.IMAGE) {
            throw ExceptionFactory.invalidParam("error.profile.avatarMustBeImage");
        }

        requireProfile(userId).setAvatarMedia(avatar);
    }

    @Override
    @Transactional
    public void removeAvatar(UUID userId) {
        requireProfile(userId).setAvatarMedia(null);
    }

    private UserInfoEntity requireProfile(UUID userId) {
        return userInfoRepo.findOneByUserId(userId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.profile.notFound",
                        userId));
    }

    private ProfileResult toResult(UserInfoEntity profile) {
        ProfileResult result = mapper.map(profile, ProfileResult.class);
        result.setEmail(profile.getUser().getEmail());

        if (profile.getAvatarMedia() != null) {
            result.setAvatar(mediaSvc.getOwnedMediaDetail(
                    profile.getAvatarMedia().getId(),
                    profile.getUserId()));
        }

        return result;
    }
}
