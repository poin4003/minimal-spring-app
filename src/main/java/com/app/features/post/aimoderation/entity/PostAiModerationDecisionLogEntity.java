package com.app.features.post.aimoderation.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.entity.PostEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
        name = "post_ai_moderation_decision_log",
        indexes = {
                @Index(
                        name = "idx_post_ai_moderation_log_post_created_at",
                        columnList = "post_id, created_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class PostAiModerationDecisionLogEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "outcome", nullable = false)
    private PostAiModerationOutcome outcome;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT", nullable = false)
    private String promptSnapshot;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "model_name", length = 255)
    private String modelName;
}
