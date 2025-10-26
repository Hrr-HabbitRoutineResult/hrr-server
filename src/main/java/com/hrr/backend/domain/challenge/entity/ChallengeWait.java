package com.hrr.backend.domain.challenge.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class ChallengeWait extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
