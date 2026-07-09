package com.app.features.rbac.sync;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.DefaultRoleConstants;
import com.app.core.constant.PermissionConstants;
import com.app.core.sync.SyncableDataService;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.rbac.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacSyncService implements SyncableDataService {

    private final PermissionRepository permRepo;
    private final RoleRepository roleRepo;

    @Override
    public String getSyncType() {
        return "SYNC_RBAC_DATA";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToDatabase() {
        log.info("[Sync] Scanning for RBAC Permissions defination...");
        int insertCount = 0;

        List<String> codePermissions = extractPermissionsFromConstants();
        for (String pKey : codePermissions) {
            if (!permRepo.existsByKey(pKey)) {
                PermissionEntity perm = new PermissionEntity();
                perm.setKey(pKey);
                perm.setName("Permission " + pKey);
                permRepo.save(perm);
                log.debug("[Sync] Inserted new Permission: [{}]", pKey);
                insertCount++;
            }
        }

        // Default role of system
        RoleEntity superAdminRole = roleRepo.findByKey(DefaultRoleConstants.SUPER_ADMIN)
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity();
                    r.setKey(DefaultRoleConstants.SUPER_ADMIN);
                    r.setName("Role " + DefaultRoleConstants.SUPER_ADMIN);
                    log.info("[Sync] Seeded SUPER_ADMIN role");
                    return roleRepo.save(r);
                });

        List<PermissionEntity> allPermissions = permRepo.findAll();
        superAdminRole.setPermissions(new HashSet<>(allPermissions));

        roleRepo.save(superAdminRole);

        log.info(">>> Sync [{}] COMPLETE. Inserted new permissions: {}", getSyncType(), insertCount);
    }

    private List<String> extractPermissionsFromConstants() {
        return Arrays.stream(PermissionConstants.class.getDeclaredFields())
                .map(field -> {
                    try {
                        return (String) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
