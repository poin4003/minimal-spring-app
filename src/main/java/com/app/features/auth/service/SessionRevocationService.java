package com.app.features.auth.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface SessionRevocationService {

    Set<UUID> findUserIdsByRoleId(UUID roleId);

    void revokeSessionsByUserId(UUID userId);

    void revokeSessionsByUserIds(Collection<UUID> userIds);
}
