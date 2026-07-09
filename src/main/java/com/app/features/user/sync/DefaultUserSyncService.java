package com.app.features.user.sync;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.DefaultRoleConstants;
import com.app.core.exception.ExceptionFactory;
import com.app.core.sync.SyncableDataService;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserSyncService implements SyncableDataService {

    private final RoleRepository roleRepo;
    private final UserBaseRepository userRepo;
    private final UserService userSvc;

    @Override
    public String getSyncType() {
        return "SYNC_DEFAULT_USER_DATA";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToDatabase() {
        log.info("[Sync] Scanning for default user defination...");

        UserBaseEntity superAdminUser = userSvc.getOrInitDefaultSystemAdmin();

        if (superAdminUser.getId() == null) {
            log.info("[Sync] Seeding new system admin account: {}", superAdminUser.getEmail());

            RoleEntity superAdminRole = roleRepo.findByKey(DefaultRoleConstants.SUPER_ADMIN)
                    .orElseThrow(() -> ExceptionFactory
                            .notFound("Role Super Admin not found! Please check RbacSyncService."));

            superAdminUser.setRoles(Set.of(superAdminRole));

            userRepo.save(superAdminUser);
        } else {
            log.debug("[Sync] System admin account already exists: {}", superAdminUser.getEmail());
        }

        log.info(">>> Sync [{}] COMPLETE.", getSyncType());
    }
}
