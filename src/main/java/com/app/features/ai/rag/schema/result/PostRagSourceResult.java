package com.app.features.ai.rag.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.post.enums.PostType;

import lombok.Data;

@Data
public class PostRagSourceResult {

    private int rank;
    private UUID postId;
    private PostType postType;
    private float score;
    private LocalDateTime sourceUpdatedAt;
    private String content;
}
