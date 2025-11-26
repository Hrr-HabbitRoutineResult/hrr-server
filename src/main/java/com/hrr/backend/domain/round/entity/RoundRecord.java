package com.hrr.backend.domain.round.entity;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserVerificationStatus;
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
@Table(name = "round_record",
        indexes = @Index(name = "idx_round_rank", columnList = "round_id, verification_count DESC"))
public class RoundRecord extends BaseEntity {

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
    private List<Verification> verifications = new ArrayList<>();

    // 비즈니스 메서드
    public void increaseVerificationCount() {
        this.verificationCount++;
    }

    public void updateFinalRank(Integer rank) {
        this.finalRank = rank;
    }

    public void updateNextRoundIntent(NextRoundIntent intent) {
        this.nextRoundIntent = intent;
    }
}