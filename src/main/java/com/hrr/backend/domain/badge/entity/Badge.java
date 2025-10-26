package com.hrr.backend.domain.badge.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Badge extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
