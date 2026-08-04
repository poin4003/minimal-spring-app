package com.app.features.post.standard.schema.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.app.features.post.enums.PostType;
import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.user.schema.result.UserPublicResult;

import lombok.Data;

@Data
public class PublicStandardPostResult {
    
    private UUID id;

    private PostType type;

    private UserPublicResult author;

    private String content;

    private List<PostMediaResult> media = List.of();

    private LocalDateTime publishedAt;
}
