package com.hrr.backend.domain.report.entity;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.enums.ReportReason;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "challenge_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_challenge_report_reporter_challenge",
                        columnNames = {"reporter_id", "challenge_id"}
                )
        },
        indexes = {
                @Index(name = "idx_challenge_report_challenge_id", columnList = "challenge_id")
        }
)
public class ChallengeReport extends BaseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Builder
    public ChallengeReport(User reporter, ReportReason reason, String description, Challenge challenge) {
        super(reporter, reason, description);
        this.challenge = challenge;
    }
}
