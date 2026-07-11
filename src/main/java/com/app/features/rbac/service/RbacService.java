package com.app.features.rbac.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.payload.CreateRolePayload;
import com.app.features.rbac.schema.payload.UpdateRolePayload;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;

public interface RbacService {

    RoleResult createRole(CreateRolePayload payload);

    void deleteRole(UUID roleId);

    RoleResult updateRole(UUID roleId, UpdateRolePayload payload);

    void assignRoleToUser(UUID userId, List<UUID> roleIds);

    void assignPermToRole(UUID roleId, List<UUID> permIds);

    void removeRoleFromUser(UUID userId, List<UUID> roleIds);

    void removePermFromRole(UUID roleId, List<UUID> permIds);

    Page<PermissionResult> getManyPermissions(PermissionFilterCriteria criteria, Pageable pageable);

    Page<RoleResult> getManyRoles(RoleFilterCriteria criteria, Pageable pageable);
}
