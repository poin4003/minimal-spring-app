package com.app.features.user.entity;

import org.springframework.security.core.userdetails.UserDetails;

import com.app.core.base.BaseEntity;
import com.app.features.user.enums.UserStatusEnum;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseUserDetailEntity extends BaseEntity implements UserDetails {
    public abstract String getDelFlag();
    public abstract UserStatusEnum getUserStatus();

    @Override
    public boolean isAccountNonExpired() {
        return true;    
    }

    @Override
    public boolean isAccountNonLocked() { 
        return getUserStatus() != UserStatusEnum.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "0".equals(getDelFlag());
    }
}
