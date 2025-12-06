package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "verification",
        indexes = {
                // 날짜 범위 검색 최적화
                @Index(name = "idx_verification_created_at", columnList = "created_at"),
                // 상태값 필터링 최적화
                @Index(name = "idx_verification_status", columnList = "status")
        }
)
public class Verification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_record_id", nullable = false)
    private RoundRecord roundRecord;

    /** 유저 챌린지 (develop 추가) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_challenge_id")
    private UserChallenge userChallenge;

    @Enumerated(EnumType.STRING)
    private VerificationPostType type; // CAMERA, TEXT

    /** 이 인증이 속한 라운드 ID (조회 편의용) */
    private Long roundId;

    private String title;

    @Lob
    private String content;

    private String photoUrl;

    private String textUrl;

    private Boolean isQuestion;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // TEMPORARY, COMPLETED

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private Boolean isResolved = false;

    /** TEXT 인증 생성 */
    public static Verification createTextVerification(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            String title,
            String content,
            String textUrl,
            String photoUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .type(VerificationPostType.TEXT)
                .roundRecord(roundRecord)
                .userChallenge(userChallenge) // develop 필드
                .roundId(roundId)
                .title(title)
                .content(content)
                .textUrl(textUrl)
                .photoUrl(photoUrl)
                .isQuestion(isQuestion)
                .status(VerificationStatus.TEMPORARY)
                .isResolved(false) // feat/90 필드 초기화
                .build();
    }

    /** PHOTO 인증 생성 */
    public static Verification createPhotoVerification(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            String title,
            String content,
            String photoUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .type(VerificationPostType.CAMERA)
                .roundRecord(roundRecord)
                .userChallenge(userChallenge) // develop 필드
                .roundId(roundId)
                .title(title)
                .content(content)
                .photoUrl(photoUrl)
                .isQuestion(isQuestion)
                .status(VerificationStatus.TEMPORARY)
                .isResolved(false) // feat/90 필드 초기화
                .build();
    }

    // === 비즈니스 로직 메서드 ===

    public void resolve() {
        this.isResolved = true;
    }

    // develop: 편의 메서드
    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public void setUserChallenge(UserChallenge uc) {
        this.userChallenge = uc;
    }
}