package com.app.features.user.service;

import java.util.UUID;

import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.schema.payload.UpdateProfilePayload;
import com.app.features.user.schema.result.ProfileResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ProfileService {

    ProfileResult getProfile(@NotNull UUID userId);

    ProfileResult updateProfile(
            @NotNull UUID userId,
            @NotNull @Valid UpdateProfilePayload payload);

    ProfileResult updateTheme(
            @NotNull UUID userId,
            boolean darkThemeEnabled);

    void updateAvatar(
            @NotNull UUID userId,
            @NotNull UUID avatarMediaId);

    void removeAvatar(@NotNull UUID userId);

    UserInfoEntity requireProfile(UUID userId);
}
