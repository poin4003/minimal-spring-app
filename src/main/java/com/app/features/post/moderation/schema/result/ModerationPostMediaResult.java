package com.app.features.post.moderation.schema.result;

import com.app.features.media.schema.result.MediaResult;
import com.app.features.post.enums.PostMediaRole;

import lombok.Data;

@Data
public class ModerationPostMediaResult {
    
    private PostMediaRole role;

    private int position;

    private MediaResult media;
}
