package com.hrr.backend.domain.challenge.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "challenge_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_challenge_like_user_challenge",
                        columnNames = {"user_id", "challenge_id"}
                )
        }
)
public class ChallengeLike extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Builder
    public ChallengeLike(User user, Challenge challenge) {
        this.user = user;
        this.challenge = challenge;
    }
}