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
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final UserBaseRepository userBaseRepo;
    private final ModelMapper mapper;

    @Override
    public RoleResult createRole(CreateRolePayload payload) {
        if (roleRepo.existsByKey(payload.getKey())) {
            throw ExceptionFactory.alreadyExists("key", payload.getKey(), "Role key already exists.");
        }

        RoleEntity role = new RoleEntity();
        role.setName(payload.getName());
        role.setKey(payload.getKey());

        roleRepo.save(role);

        return mapper.map(role, RoleResult.class);
    }

    @Override
    public void deleteRole(UUID roleId) {
        if (!roleRepo.existsById(roleId)) {
            throw ExceptionFactory.notFound("Role: " + roleId);
        }

        roleRepo.deleteById(roleId);
    }

    @Override
    public RoleResult getRole(UUID roleId) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound("Role: " + roleId));

        return mapper.map(role, RoleResult.class);
    }

    @Override
    @Transactional
    public RoleResult updateRole(UUID roleId, UpdateRolePayload payload) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound("Role: " + roleId));

        mapper.getConfiguration().setSkipNullEnabled(true);
        mapper.map(payload, role);

        role = roleRepo.save(role);

        return mapper.map(role, RoleResult.class);
    }

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, List<UUID> roleIds) {
        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User id: " + userId));

        List<RoleEntity> roles = roleRepo.findAllById(roleIds);

        if (roles.size() != roleIds.size()) {
            throw ExceptionFactory.notFound("Missing some role");
        }

        HashSet<RoleEntity> currentRoles = user.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getRoles());
        currentRoles.addAll(roles);

        user.setRoles(currentRoles);

        userBaseRepo.save(user);
    }

    @Override
    @Transactional
    public void assignPermToRole(UUID roleId, List<UUID> permIds) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound("Role id: " + roleId));

        List<PermissionEntity> perms = permRepo.findAllById(permIds);

        if (perms.size() != permIds.size()) {
            throw ExceptionFactory.notFound("Missing some permission");
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
    public void removeRoleFromUser(UUID userId, List<UUID> roleIds) {
        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User id: " + userId));

        List<RoleEntity> roles = roleRepo.findAllById(roleIds);

        HashSet<RoleEntity> currentRoles = user.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getRoles());
        currentRoles.removeAll(roles);

        user.setRoles(currentRoles);

        userBaseRepo.save(user);
    }

    @Override
    @Transactional
    public void removePermFromRole(UUID roleId, List<UUID> permIds) {
        RoleEntity role = roleRepo.findById(roleId)
                .orElseThrow(() -> ExceptionFactory.notFound("Role id: " + roleId));

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
