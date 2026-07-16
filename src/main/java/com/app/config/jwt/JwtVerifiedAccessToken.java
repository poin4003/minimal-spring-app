package com.app.config.jwt;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtVerifiedAccessToken {

    private final UUID userId;
    private final JwtAccessPayload payload;
}
