package com.hrr.backend.domain.user.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
public class UserBadge extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
