package com.app.features.auth.api.v1.dto.response;

import lombok.Data;

@Data
public class LoginResponseDto {

    private String accessToken;

    private String refreshToken;
}
