package com.app.features.user.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class UserShortResult {

    private UUID id;

    private String email;
}
