package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "verification_scrap",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_verification_scrap_user_verification", columnNames = {"user_id", "verification_id"})
        },
        indexes = {
                @Index(name = "idx_verification_scrap_user_id", columnList = "user_id"),
                @Index(name = "idx_verification_scrap_verification_id", columnList = "verification_id")
        }
)
public class VerificationScrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private Verification verification;

    public static VerificationScrap create(User user, Verification verification) {
        return VerificationScrap.builder()
                .user(user)
                .verification(verification)
                .build();
    }
}
