package com.app.features.user.service.impl;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.core.exception.ExceptionFactory;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.schema.payload.CreateUserPayload;
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.schema.result.UserResult;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserBaseRepository userBaseRepo;
    private final PermissionRepository permRepo;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;

    @Override
    public UserResult createUser(CreateUserPayload payload) {
        checkEmailUnique(payload.getEmail());

        UserBaseEntity userBase = new UserBaseEntity();
        userBase.setEmail(payload.getEmail());
        userBase.setPassword(passwordEncoder.encode(payload.getPassword()));
        userBase.setStatus(UserStatusEnum.ACTIVE);
        userBase.setDelFlag("0");

        userBase = userBaseRepo.save(userBase);

        return mapper.map(userBase, UserResult.class);
    }

    @Override
    public Page<UserResult> getManyUser(Pageable pageable) {
        Page<UserBaseEntity> entityPage = userBaseRepo.findAll(pageable);

        return entityPage.map(result -> mapper.map(result, UserResult.class));
    }

    @Override
    public UserDetailResult getUserDetailById(UUID userId) {
        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + userId));

        // LAZY load List role
        UserDetailResult response = mapper.map(user, UserDetailResult.class);

        Set<PermissionEntity> perms = permRepo.findAllByUserId(userId);
        response.setPermissions(perms.stream()
                .map(perm -> mapper.map(perm, PermissionResult.class))
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public void checkEmailUnique(String email) {
        if (userBaseRepo.existsByEmail(email)) {
            throw ExceptionFactory.alreadyExists("Email " + email);
        }
    }
}
