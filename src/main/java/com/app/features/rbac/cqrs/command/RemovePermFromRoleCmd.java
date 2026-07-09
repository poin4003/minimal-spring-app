package com.app.features.rbac.cqrs.command;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.rbac.repository.RoleRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record RemovePermFromRoleCmd(
        UUID roleId,
        List<UUID> permIds) implements Command<Void> {
}

@Component
@RequiredArgsConstructor
class RemovePermissionToRoleHandler implements Command.Handler<RemovePermFromRoleCmd, Void> {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    @Override
    @Transactional
    public Void handle(RemovePermFromRoleCmd cmd) {
        RoleEntity role = roleRepo.findById(Objects.requireNonNull(cmd.roleId()))
                .orElseThrow(() -> ExceptionFactory.notFound("Role id: " + cmd.roleId()));

        List<PermissionEntity> perms = permRepo.findAllById(Objects.requireNonNull(cmd.permIds()));

        role.getPermissions().removeAll(perms);

        roleRepo.save(role);

        return null;
    }
}
