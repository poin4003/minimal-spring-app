package com.app.features.ai.rag.mapper;

import org.springframework.stereotype.Component;

import com.app.features.ai.rag.schema.model.PostRagGeneratedAnswer;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.schema.model.PostRagSource;
import com.app.features.ai.rag.schema.result.PostRagAnswerResult;
import com.app.features.ai.rag.schema.result.PostRagSourceResult;

@Component
public class PostRagResultMapper {

    public PostRagAnswerResult toResult(PostRagResult source) {
        PostRagAnswerResult result = new PostRagAnswerResult();
        result.setQuestion(source.question());
        result.setRetrievalAvailability(
                source.retrievalAvailability());
        result.setGenerationAvailability(
                source.generationAvailability());
        result.setGenerated(source.hasGeneratedAnswer());
        result.setSources(source.sources().stream()
                .map(item -> toSourceResult(item))
                .toList());

        PostRagGeneratedAnswer generatedAnswer =
                source.generatedAnswer();
        if (generatedAnswer != null) {
            result.setAnswer(generatedAnswer.text());
            result.setModelId(generatedAnswer.modelId());
        }

        return result;
    }

    private PostRagSourceResult toSourceResult(PostRagSource source) {
        PostRagSourceResult result = new PostRagSourceResult();
        result.setRank(source.rank());
        result.setPostId(source.postId());
        result.setPostType(source.postType());
        result.setScore(source.score());
        result.setSourceUpdatedAt(source.sourceUpdatedAt());
        result.setContent(source.content());
        return result;
    }
}
