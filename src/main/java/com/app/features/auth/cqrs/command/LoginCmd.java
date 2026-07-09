package com.app.features.auth.cqrs.command;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.cqrs.result.LoginResult;
import com.app.features.auth.service.AuthService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record LoginCmd(
        String email,
        String password,
        String ipAddress) implements Command<LoginResult> {
}

@Component
@RequiredArgsConstructor
class LoginHandler implements Command.Handler<LoginCmd, LoginResult> {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserBaseRepository userBaseRepo;

    @Override
    public LoginResult handle(LoginCmd cmd) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cmd.email(), cmd.password()));

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        String userEmail = userDetails.getEmail();
        
        String email = Objects.requireNonNull(cmd.email(), "Email must be not null");

        UserBaseEntity user = userBaseRepo.findByEmail(email)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + userId));

        updateUserLoginInfo(user, cmd.ipAddress());

        return authService.generateAndSaveTokens(userId, userEmail);
    }

    private void updateUserLoginInfo(UserBaseEntity user, String ipAddress) {
        user.setLoginTime(LocalDateTime.now());
        user.setLoginIp(ipAddress);

        userBaseRepo.save(user);
    }
}
