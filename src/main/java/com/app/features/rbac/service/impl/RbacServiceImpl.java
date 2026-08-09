package com.app.features.rbac.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.exception.ExceptionFactory;
import com.app.core.security.session.RevokeSessions;
import com.app.core.security.session.SessionRevocationScope;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.rbac.repository.spec.PermissionSpecification;
import com.app.features.rbac.repository.spec.RoleSpecification;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.payload.CreateRolePayload;
import com.app.features.rbac.schema.payload.UpdateRolePayload;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final UserService userSvc;
    private final ModelMapper mapper;

    @Override
    public RoleResult createRole(CreateRolePayload payload) {
        if (roleRepo.existsByKey(payload.getKey())) {
            throw ExceptionFactory.alreadyExists(
                    "key",
                    payload.getKey(),
                    "error.rbac.roleKeyAlreadyExists");
        }

        RoleEntity role = new RoleEntity();
        role.setName(payload.getName());
        role.setKey(payload.getKey());

        roleRepo.save(role);

        return mapper.map(role, RoleResult.class);
    }

    @Override
    @RevokeSessions(scope = SessionRevocationScope.USERS_BY_ROLE)
    public void deleteRole(UUID roleId) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.rbac.roleNotFound",
                        roleId));

        roleRepo.delete(role);
    }

    @Override
    public RoleResult getRole(UUID roleId) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.rbac.roleNotFound",
                        roleId));

        return mapper.map(role, RoleResult.class);
    }

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USERS_BY_ROLE)
    public RoleResult updateRole(UUID roleId, UpdateRolePayload payload) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.rbac.roleNotFound",
                        roleId));

        if (payload.getKey() != null
                && !payload.getKey().equals(role.getKey())
                && roleRepo.existsByKey(payload.getKey())) {
            throw ExceptionFactory.alreadyExists(
                    "key",
                    payload.getKey(),
                    "error.rbac.roleKeyAlreadyExists");
        }

        mapper.map(payload, role);

        role = roleRepo.save(role);

        return mapper.map(role, RoleResult.class);
    }

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USER)
    public void assignRoleToUser(UUID userId, List<UUID> roleIds) {
        UserBaseEntity user = userSvc.requireUser(userId);

        List<RoleEntity> roles = roleRepo.findAllById(roleIds);

        if (roles.size() != roleIds.size()) {
            throw ExceptionFactory.notFound("error.rbac.rolesMissing");
        }

        HashSet<RoleEntity> currentRoles = user.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getRoles());
        currentRoles.addAll(roles);

        user.setRoles(currentRoles);
    }

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USERS_BY_ROLE)
    public void assignPermToRole(UUID roleId, List<UUID> permIds) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.rbac.roleNotFound",
                        roleId));

        List<PermissionEntity> perms = permRepo.findAllById(permIds);

        if (perms.size() != permIds.size()) {
            throw ExceptionFactory.notFound("error.rbac.permissionsMissing");
        }

        HashSet<PermissionEntity> currentPermissions = role.getPermissions() == null
                ? new HashSet<>()
                : new HashSet<>(role.getPermissions());
        currentPermissions.addAll(perms);

        role.setPermissions(currentPermissions);

        roleRepo.save(role);
    }

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USER)
    public void removeRoleFromUser(UUID userId, List<UUID> roleIds) {
        UserBaseEntity user = userSvc.requireUser(userId);

        List<RoleEntity> roles = roleRepo.findAllById(roleIds);

        HashSet<RoleEntity> currentRoles = user.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getRoles());
        currentRoles.removeAll(roles);

        user.setRoles(currentRoles);
    }

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USERS_BY_ROLE)
    public void removePermFromRole(UUID roleId, List<UUID> permIds) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.rbac.roleNotFound",
                        roleId));

        List<PermissionEntity> perms = permRepo.findAllById(permIds);

        HashSet<PermissionEntity> currentPermissions = role.getPermissions() == null
                ? new HashSet<>()
                : new HashSet<>(role.getPermissions());
        currentPermissions.removeAll(perms);

        role.setPermissions(currentPermissions);

        roleRepo.save(role);
    }

    @Override
    public Page<PermissionResult> getManyPermissions(PermissionFilterCriteria criteria, Pageable pageable) {
        Specification<PermissionEntity> spec = PermissionSpecification.withFilter(criteria);

        Page<PermissionEntity> entityPage = permRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, PermissionResult.class));
    }

    @Override
    public Page<RoleResult> getManyRoles(RoleFilterCriteria criteria, Pageable pageable) {
        Specification<RoleEntity> spec = RoleSpecification.withFilter(criteria);

        Page<RoleEntity> entityPage = roleRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, RoleResult.class));
    }
}
