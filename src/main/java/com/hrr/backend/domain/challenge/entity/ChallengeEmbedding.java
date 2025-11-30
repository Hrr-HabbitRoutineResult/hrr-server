package com.hrr.backend.domain.challenge.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "challenge_embedding")
public class ChallengeEmbedding extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, unique = true)
    private Challenge challenge;

    @Lob
    @Column(name = "challenge_text", nullable = false)
    private String challengeText;

    @Lob
    @Column(name = "challenge_embedding", columnDefinition = "LONGBLOB")
    private byte[] challengeEmbedding;

}
