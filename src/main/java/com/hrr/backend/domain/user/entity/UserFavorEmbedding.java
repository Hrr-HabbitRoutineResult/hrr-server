package com.hrr.backend.domain.user.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_favor_embedding")
@Builder
public class UserFavorEmbedding extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favor_id", nullable = false, unique = true)
    private UserFavor userFavor;

    @Lob
    @Column(name = "favor_text", nullable = false)
    private String favorText;

    @Lob
    @Column(name = "favor_embedding")
    private byte[] favorEmbedding;

}
