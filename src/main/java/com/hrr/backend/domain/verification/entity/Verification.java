package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Verification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_record_id", nullable = false)
    private RoundRecord roundRecord;
}
