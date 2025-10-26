package com.hrr.backend.domain.keyword.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Keyword extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
