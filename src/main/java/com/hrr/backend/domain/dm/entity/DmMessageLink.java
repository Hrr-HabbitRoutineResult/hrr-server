package com.hrr.backend.domain.dm.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dm_message_link")
public class DmMessageLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private DmMessage message;

    @NotNull
    @Column(name = "url", length = 2048, nullable = false)
    private String url;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

}
