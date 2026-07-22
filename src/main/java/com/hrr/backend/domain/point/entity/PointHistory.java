package com.hrr.backend.domain.point.entity;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "point_history",
        indexes = {
                @Index(name = "idx_point_history_user_created", columnList = "user_id, created_at")
        }
)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false, length = 30)
    private PointType pointType;

    @Column(name = "points", nullable = false)
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_mission_id")
    private RandomMission randomMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private Verification verification;

    /** 포인트 적립 기록 생성 팩토리 메서드 */
    public static PointHistory of(User user, PointType pointType, Challenge challenge, Round round, RandomMission randomMission, Verification verification) {
        return PointHistory.builder()
                .user(user)
                .pointType(pointType)
                .points(pointType.getPoints())
                .challenge(challenge)
                .round(round)
                .randomMission(randomMission)
                .verification(verification)
                .build();
    }
}