package com.app.features.auth.service.impl;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.security.UserPrincipal;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

        private final UserBaseRepository userBaseRepo;

        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // 1. Get user info
                UserBaseEntity user = userBaseRepo.findByEmail(username)
                                .orElseThrow(() -> new UsernameNotFoundException("Not found" + username));

                // 2. Get roles
                Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

                return UserPrincipal.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .password(user.getPassword())
                                .status(user.getStatus())
                                .delFlag(user.getDelFlag())
                                .authorities(authorities)
                                .build();
        }
}
