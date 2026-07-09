package com.app.features.user.cqrs.query;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.cqrs.result.PermissionResult;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.user.cqrs.result.UserDetailResult;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record GetUserByIdQuery(UUID userId) implements Command<UserDetailResult> {
}

@Component
@RequiredArgsConstructor
class GetuserByIdHanlder implements Command.Handler<GetUserByIdQuery, UserDetailResult> {

    private final ModelMapper mapper;
    private final UserBaseRepository userRepo;
    private final PermissionRepository permRepo;

    @Override
    public UserDetailResult handle(GetUserByIdQuery query) {
        UUID id = Objects.requireNonNull(query.userId(), "User id must be not null");

        UserBaseEntity user = userRepo.findById(id)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + query.userId()));

        // LAZY load List role
        UserDetailResult result = mapper.map(user, UserDetailResult.class);

        Set<PermissionEntity> perms = permRepo.findAllByUserId(id);
        result.setPermissions(perms.stream()
                .map(perm -> mapper.map(perm, PermissionResult.class))
                .collect(Collectors.toList()));

        return result;
    }
}
