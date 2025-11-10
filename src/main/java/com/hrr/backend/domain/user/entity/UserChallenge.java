package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeRole;
import com.hrr.backend.domain.user.entity.enums.VerificationStatus;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_challenge")
public class UserChallenge extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @ColumnDefault("'CHALLENGER'")
    @Builder.Default
    private ChallengeRole role = ChallengeRole.CHALLENGER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @NotNull
    @Column(name = "verification_count", nullable = false)
    private Integer verificationCount;

    @NotNull
    @Column(name = "verification_uncount", nullable = false)
    private Integer verificationUncount;

    @NotNull
    @Column(name = "warn_count", nullable = false)
    private Integer warnCount;
}
