package com.app.features.post.standard.schema.result;

import java.util.List;
import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class PublicStandardPostResult {

    private PostSummaryResult post;

    private String content;

    private List<PostMediaResult> media = List.of();
}
