package com.hrr.backend.domain.term.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Term extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
