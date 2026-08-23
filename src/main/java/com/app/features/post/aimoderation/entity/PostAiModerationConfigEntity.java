package com.app.features.post.aimoderation.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.core.db.BaseAuditEntity;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "post_ai_moderation_config",
        indexes = {
                @Index(
                        name = "uk_post_ai_moderation_config_code",
                        columnList = "code",
                        unique = true)
        })
@Data
@EqualsAndHashCode(callSuper = true)
public class PostAiModerationConfigEntity extends BaseAuditEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mode", nullable = false)
    private PostAiModerationMode mode;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;
}
