package com.hrr.backend.domain.round.entity;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "round",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_challenge_round_number",
                        columnNames = {"challenge_id", "round_number"}
                )
        },
        indexes = {
                @Index(name = "idx_round_end_date", columnList = "endDate")
        }
)
public class Round extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

}