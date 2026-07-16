package com.app.core.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.app.features.user.enums.UserStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    
    private UUID userId;
    private String email;

    @JsonIgnore
    private KeyStoreResult keyStore;

    private UserStatusEnum status;

    private Collection<? extends GrantedAuthority> authorities;
}
