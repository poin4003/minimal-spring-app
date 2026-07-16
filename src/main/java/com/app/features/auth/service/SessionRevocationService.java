package com.app.features.auth.service;

import java.util.UUID;

public interface SessionRevocationService {

    void revokeSessionsByUserId(UUID userId);

    void revokeSessionsByRoleId(UUID roleId);
}
