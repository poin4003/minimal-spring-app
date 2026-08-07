package com.app.features.post.moderation.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.post.moderation.service.PostModerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostModerationServiceImpl implements PostModerationService {@Override

    

    public ModerationStandardPostDetailResult getStandardPostDetail(UUID postId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStandardPostDetail'");
    }

    @Override
    public void publishedPost(UUID postId, UUID moderatorId) {
    }

    @Override
    public void rejectPost(UUID postId, UUID moderatorId, @Valid RejectPostPayload payload) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'rejectPost'");
    }


    
}
