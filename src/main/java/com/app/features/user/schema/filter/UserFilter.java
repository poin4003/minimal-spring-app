package com.app.features.user.schema.filter;

import org.springframework.util.StringUtils;

import com.app.features.user.enums.UserStatusEnum;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilter {

    @Size(max = 100, message = "{validation.filter.keyword.max}")
    private String keyword;

    private UserStatusEnum status;

    public boolean hasKeyword() {
        return StringUtils.hasText(keyword);
    }

    public boolean hasStatus() {
        return status != null;
    }
}
