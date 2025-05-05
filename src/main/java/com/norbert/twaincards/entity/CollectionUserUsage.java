package com.norbert.twaincards.entity;

import lombok.*;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_user_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionUserUsage {

    @EmbeddedId
    private CollectionUserUsageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("collectionId")
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_used_at")
    private LocalDateTime firstUsedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "use_count")
    private Integer useCount;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionUserUsageId implements Serializable {
        @Column(name = "collection_id")
        private Long collectionId;

        @Column(name = "user_id")
        private Long userId;
    }
} 