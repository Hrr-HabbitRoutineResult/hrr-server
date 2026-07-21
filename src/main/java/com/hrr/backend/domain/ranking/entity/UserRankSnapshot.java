package com.hrr.backend.domain.ranking.entity;

import java.time.LocalDate;

import com.hrr.backend.domain.user.entity.User;
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
        name = "user_rank_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_snapshot_date",
                        columnNames = {"user_id", "snapshot_date"}
                )
        },
        indexes = {
                @Index(name = "idx_snapshot_date_ranking", columnList = "snapshot_date, ranking")
        }
)
public class UserRankSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ranking", nullable = false)
    private Integer ranking;

    // 스냅샷 시점의 포인트 (랭킹보드에 고정 노출되는 값)
    @Column(name = "points", nullable = false)
    private Long points;

    // 스냅샷 시점의 전체 활성 유저 수 (상위 N% 계산용)
    @Column(name = "total_user_count", nullable = false)
    private Integer totalUserCount;

    // 스냅샷 기준일 (매주 월요일)
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
}