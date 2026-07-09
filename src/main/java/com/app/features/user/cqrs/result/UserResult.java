package com.app.features.user.cqrs.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.user.enums.UserStatusEnum;

import lombok.Data;

@Data
public class UserResult {

    private UUID id;

    private String email;

    private UserStatusEnum status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
