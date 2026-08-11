package com.hrr.backend.domain.round.entity;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "round_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_round_user",
                        columnNames = {"round_id", "user_challenge_id"}
                )
        },
        indexes = {
                // 검색용 기본 인덱스
                @Index(name = "idx_round_id", columnList = "round_id"),

                // 내 히스토리 조회용 인덱스
                @Index(name = "idx_user_history", columnList = "user_challenge_id")
        }
)
public class RoundRecord extends BaseEntity {
	@Version
	private Long version; // for 낙관적 락

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_challenge_id", nullable = false)
    private UserChallenge userChallenge;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Integer verificationCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Integer warnCount = 0;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NextRoundIntent nextRoundIntent = NextRoundIntent.UNDECIDED;

    // 양방향 매핑
    @OneToMany(mappedBy = "roundRecord", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Verification> verifications = new ArrayList<>();

    // 비즈니스 메서드
    public void increaseVerificationCount() {
        this.verificationCount++;
    }

    // 인증글 삭제 시 인증 횟수를 감소
    // 기존 데이터에 삭제분이 반영되어 있지 않아 음수가 발생할 수 있으므로 하한(0) 처리
    public void decreaseVerificationCount() {
        if (this.verificationCount > 0) {
            this.verificationCount--;
        }
    }

    public void updateFinalRank(Integer rank) {
        this.finalRank = rank;
    }

    public void updateNextRoundIntent(NextRoundIntent intent) {
        this.nextRoundIntent = intent;
    }

	// 데이터 정합성을 고려하여 경고 횟수를 업데이트
    public void synchronizeWarnCount(int newWarnCount) {
        this.warnCount = newWarnCount;
	}
}
