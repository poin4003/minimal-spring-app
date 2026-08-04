package com.app.features.user.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class UserPublicResult {
    
    private UUID id;

    private String fullName;

    private String avatarUrl;
}
