package com.app.features.user.schema.result;

import java.util.UUID;

import com.app.core.enums.AppLanguage;
import com.app.features.media.schema.result.MediaResult;

import lombok.Data;

@Data
public class ProfileResult {

    private UUID userId;
    private String email;
    private String fullName;
    private AppLanguage language;
    private boolean darkThemeEnabled;
    private MediaResult avatar;
}
