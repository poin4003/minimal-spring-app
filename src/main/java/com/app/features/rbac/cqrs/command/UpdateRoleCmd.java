package com.app.features.rbac.cqrs.command;

import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.cqrs.result.RoleResult;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record UpdateRoleCmd(
        UUID id,
        String name,
        String key) implements Command<RoleResult> {
}

@Component
@RequiredArgsConstructor
class UpdateRoleHandler implements Command.Handler<UpdateRoleCmd, RoleResult> {

    private final ModelMapper modelMapper;
    private final RoleRepository roleRepo;

    @Override
    public RoleResult handle(UpdateRoleCmd cmd) {
        RoleEntity role = roleRepo.findById(Objects.requireNonNull(cmd.id()))
                .orElseThrow(() -> ExceptionFactory.notFound("Role: " + cmd.id()));

        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(cmd, role);

        role = roleRepo.save(Objects.requireNonNull(role));

        return modelMapper.map(role, RoleResult.class);
    }
}
