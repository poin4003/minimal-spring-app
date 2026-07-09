package com.app.features.user.service;

import com.app.features.user.entity.UserBaseEntity;

public interface UserService {

    void checkEmailUnique(String email);

    UserBaseEntity getOrInitDefaultSystemAdmin();
}
