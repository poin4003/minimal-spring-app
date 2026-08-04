package com.app.features.post.schema.result;

import com.app.features.media.schema.result.PublicMediaResult;
import com.app.features.post.enums.PostMediaRole;

import lombok.Data;

@Data
public class PostMediaResult {
    
    private PostMediaRole role;

    private int position;

    private PublicMediaResult media;
}
