package com.app.features.user.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.support.MediaUrlResolver;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.schema.result.UserPublicResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPublicResultMapper {

    private final ModelMapper mapper;
    private final MediaUrlResolver mediaUrlResolver;

    public UserPublicResult toResult(
            UserBaseEntity user,
            UserInfoEntity profile) {
        UserPublicResult result = mapper.map(profile, UserPublicResult.class);
        result.setId(user.getId());

        if (profile.getAvatarMedia() != null) {
            result.setAvatarUrl(mediaUrlResolver.resolvePreviewUrl(
                    profile.getAvatarMedia()));
        }

        return result;
    }
}
