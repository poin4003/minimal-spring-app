package com.app.features.user.api.v1.dto.response;

import java.util.UUID;

import com.app.features.user.enums.UserStatusEnum;

import lombok.Data;

@Data
public class UserDto {

    private UUID id;

    private String email;

    private UserStatusEnum status;

    private String createdAt;

    private String updatedAt;
}
