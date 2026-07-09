package com.app.features.user.service.impl;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserBaseRepository userBaseRepo;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public void checkEmailUnique(String email) {
        if (!userBaseRepo.existsByEmail(email)) {
            throw ExceptionFactory.alreadyExists("Email " + email);
        }
    }

    public UserBaseEntity getOrInitDefaultSystemAdmin() {
        var config = appProperties.getSystemAdmin();
        String defaultEmail = config.getEmail();

        Optional<UserBaseEntity> existingUser = userBaseRepo.findByEmail(defaultEmail);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        UserBaseEntity newAdmin = new UserBaseEntity();
        newAdmin.setEmail(defaultEmail);
        newAdmin.setPassword(passwordEncoder.encode(config.getPassword()));
        newAdmin.setStatus(UserStatusEnum.ACTIVE);
        newAdmin.setDelFlag("0");

        return newAdmin;
    }
}
