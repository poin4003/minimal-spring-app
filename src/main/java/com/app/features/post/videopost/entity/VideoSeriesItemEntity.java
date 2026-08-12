package com.app.features.post.videopost.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "video_series_item", indexes = {
        @Index(
                name = "uk_video_series_item_position",
                columnList = "series_id, position",
                unique = true),
        @Index(
                name = "uk_video_series_item_video",
                columnList = "series_id, video_post_id",
                unique = true),
        @Index(
                name = "idx_video_series_item_video_post",
                columnList = "video_post_id")
})
@Data
public class VideoSeriesItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoSeriesEntity series;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoPostEntity videoPost;

    @PositiveOrZero
    @Column(name = "position", nullable = false)
    private int position;
}
