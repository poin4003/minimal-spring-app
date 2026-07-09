package com.app.features.rbac.cqrs.command;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.repository.RoleRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record DeleteRoleCmd(
        UUID id) implements Command<Void> {
}

@Component
@RequiredArgsConstructor
class DeleteRoleHandler implements Command.Handler<DeleteRoleCmd, Void> {

    private final RoleRepository roleRepo;

    @Override
    public Void handle(DeleteRoleCmd cmd) {
        UUID id = Objects.requireNonNull(cmd.id());

        if (!roleRepo.existsById(id)) {
            throw ExceptionFactory.notFound("Role: " + id);
        }

        roleRepo.deleteById(id);
        return null;
    }
}
