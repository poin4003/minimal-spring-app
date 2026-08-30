package com.app.features.ai.rag.schema.result;

import java.util.List;

import com.app.features.ai.enums.AiAvailability;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostRagAnswerResult {

    private String question;
    private AiAvailability retrievalAvailability;
    private AiAvailability generationAvailability;
    private boolean generated;
    private String answer;
    private String modelId;
    private List<PostRagSourceResult> sources;
}
