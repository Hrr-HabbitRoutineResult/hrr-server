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
@Table(name = "dm_message_image")
public class DmMessageImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private DmMessage message;

    @NotNull
    @Column(name = "s3_key", length = 255, nullable = false)
    private String s3Key;

    @Column(name = "origin_filename", length = 255)
    private String originFilename;

    @Column(name = "filesize_bytes")
    private Long filesizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "mimetype", length = 100)
    private String mimetype;

}
