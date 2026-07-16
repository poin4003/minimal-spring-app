package com.app.features.auth.service.impl;

import java.util.Collection;
import java.util.UUID;

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
                UserBaseEntity user = userBaseRepo.findByEmail(username)
                                .orElseThrow(() -> new UsernameNotFoundException("Not found" + username));

                return buildPrincipal(user);
        }

        @Transactional(readOnly = true)
        public UserPrincipal loadUserByUserId(UUID userId) throws UsernameNotFoundException {
                UserBaseEntity user = userBaseRepo.findByIdWithAuthorities(userId)
                                .orElseThrow(() -> new UsernameNotFoundException("Not found " + userId));

                return buildPrincipal(user);
        }

        private UserPrincipal buildPrincipal(UserBaseEntity user) {
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
