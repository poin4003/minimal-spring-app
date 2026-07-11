package com.app.features.user.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.user.schema.payload.CreateUserPayload;
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.schema.result.UserResult;

public interface UserService {

    UserResult createUser(CreateUserPayload payload);

    Page<UserResult> getManyUser(Pageable pageable);

    UserDetailResult getUserDetailById(UUID userId);

    void checkEmailUnique(String email);
}
