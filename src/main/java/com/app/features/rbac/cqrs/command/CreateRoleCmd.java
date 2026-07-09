package com.app.features.rbac.cqrs.command;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.cqrs.result.RoleResult;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record CreateRoleCmd(
        String name,
        String key) implements Command<RoleResult> {
}

@Component
@RequiredArgsConstructor
class CreateRoleHandler implements Command.Handler<CreateRoleCmd, RoleResult> {

    private final ModelMapper modelMapper;
    private final RoleRepository roleRepo;

    @Override
    public RoleResult handle(CreateRoleCmd cmd) {
        if (roleRepo.existsByKey(cmd.key())) {
            throw ExceptionFactory.alreadyExists("Role key" + cmd.key());
        }

        RoleEntity role = new RoleEntity();
        role.setName(cmd.name());
        role.setKey(cmd.key());

        roleRepo.save(role);

        return modelMapper.map(role, RoleResult.class);
    }
}
