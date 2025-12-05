package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
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
@Table(
        name = "user_challenge",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
        },
        indexes = {
                // 챌린지 ID 기준 조회 최적화 (FK)
                @Index(name = "idx_user_challenge_challenge_id", columnList = "challenge_id"),
                // 유저 ID 기준 조회 최적화
                @Index(name = "idx_user_challenge_user_id", columnList = "user_id")
        }
)
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
    private UserChallengeRole role = UserChallengeRole.CHALLENGER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'JOINED'")
    @Builder.Default
    private ChallengeJoinStatus status = ChallengeJoinStatus.JOINED;

    // '내보내기 경고' 누적 횟수 (3회 되면 status = KICKED로 변경됨)
    @Column(name = "kick_warnings", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Integer kickWarnings = 0;

}