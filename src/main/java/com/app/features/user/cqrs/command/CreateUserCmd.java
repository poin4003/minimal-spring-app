package com.app.features.user.cqrs.command;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.user.cqrs.result.UserResult;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.service.UserService;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record CreateUserCmd(String email, String password) implements Command<UserResult> {
}

@Component
@RequiredArgsConstructor
class CreateUserHandler implements Command.Handler<CreateUserCmd, UserResult> {

    private final UserBaseRepository userBaseRepo;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResult handle(CreateUserCmd cmd) {
        userService.checkEmailUnique(cmd.email());

        UserBaseEntity userBase = new UserBaseEntity();
        userBase.setEmail(cmd.email());
        userBase.setPassword(passwordEncoder.encode(cmd.password()));
        userBase.setStatus(UserStatusEnum.ACTIVE);
        userBase.setDelFlag("0");

        userBase = userBaseRepo.save(userBase);

        return modelMapper.map(userBase, UserResult.class);
    }
}
