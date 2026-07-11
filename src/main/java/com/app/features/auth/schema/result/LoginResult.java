package com.app.features.auth.schema.result;

import lombok.Data;

@Data
public class LoginResult {

    private String accessToken;

    private String refreshToken;
}
