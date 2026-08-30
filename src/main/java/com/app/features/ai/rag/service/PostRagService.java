package com.app.features.ai.rag.service;

import com.app.features.ai.rag.schema.model.PostRagResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface PostRagService {

    PostRagResult answer(
            @NotBlank @Size(max = 2000) String question);
}
