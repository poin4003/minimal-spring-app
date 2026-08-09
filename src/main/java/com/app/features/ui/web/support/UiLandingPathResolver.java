package com.app.features.ui.web.support;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.constant.DefaultRoleConstants;
import com.app.core.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UiLandingPathResolver {

    private final AppProperties appProperties;

    public String resolve(UserPrincipal currentUser) {
        return canAccessCms(currentUser)
                ? appProperties.getUi().getHomePath()
                : appProperties.getUi().getFeedPath();
    }

    public boolean canAccessCms(UserPrincipal currentUser) {
        return currentUser != null
                && currentUser.getAuthorities().stream()
                        .anyMatch(authority -> DefaultRoleConstants.SUPER_ADMIN
                                .equals(authority.getAuthority()));
    }
}
