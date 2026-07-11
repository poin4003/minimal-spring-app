package com.app.features.user.schema.result;

import java.util.UUID;

import com.app.features.user.enums.UserStatusEnum;

import lombok.Data;

@Data
public class UserResult {

    private UUID id;

    private String email;

    private UserStatusEnum status;

    private String createdAt;

    private String updatedAt;
}
