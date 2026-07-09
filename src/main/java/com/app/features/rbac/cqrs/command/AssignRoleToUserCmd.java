package com.app.features.rbac.cqrs.command;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record AssignRoleToUserCmd(
        UUID userId,
        List<UUID> roleIds) implements Command<Void> {
}

@Component
@RequiredArgsConstructor
class AssignRoleToUserHandler implements Command.Handler<AssignRoleToUserCmd, Void> {

    private final UserBaseRepository userRepo;
    private final RoleRepository roleRepo;

    @Override
    @Transactional
    public Void handle(AssignRoleToUserCmd cmd) {
        UserBaseEntity user = userRepo.findById(Objects.requireNonNull(cmd.userId()))
                .orElseThrow(() -> ExceptionFactory.notFound("User id: " + cmd.userId()));

        List<RoleEntity> roles = roleRepo.findAllById(Objects.requireNonNull(cmd.roleIds()));

        if (roles.size() != cmd.roleIds().size()) {
            throw ExceptionFactory.notFound("Missing some role");
        }

        user.setRoles(new HashSet<>(roles));

        userRepo.save(user);

        return null;
    }
}
