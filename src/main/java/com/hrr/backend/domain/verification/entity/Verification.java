package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 인증 방식 (PHOTO / TEXT) */
    @Enumerated(EnumType.STRING)
    private VerificationPostType type;

    /** 제목 */
    private String title;

    /** 내용(텍스트 인증 전용) */
    @Lob
    private String content;

    /** 사진 인증 URL */
    private String photoUrl;

    /** 텍스트 인증 URL */
    private String textUrl;

    /** 질문 여부 */
    private Boolean isQuestion;

    /** 상태 */
    @Enumerated(EnumType.STRING)
    private VerificationStatus status;

    /** 라운드 ID (선택) */
    private Long roundId;

    /** 유저 챌린지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_challenge_id")
    private UserChallenge userChallenge;

    /** 텍스트 인증 생성 */
    public static Verification createTextVerification(
            UserChallenge userChallenge,
            String title,
            String content,
            String textUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .type(VerificationPostType.TEXT)
                .title(title)
                .content(content)
                .textUrl(textUrl)
                .isQuestion(isQuestion)
                .userChallenge(userChallenge)
                .roundId(roundId)
                .status(VerificationStatus.TEMPORARY)
                .build();
    }

    /** 사진 인증 생성 */
    public static Verification createPhotoVerification(
            UserChallenge userChallenge,
            String title,
            String photoUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .type(VerificationPostType.CAMERA)
                .title(title)
                .photoUrl(photoUrl)
                .isQuestion(isQuestion)
                .userChallenge(userChallenge)
                .roundId(roundId)
                .status(VerificationStatus.TEMPORARY)
                .build();
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public void setUserChallenge(UserChallenge uc) {
        this.userChallenge = uc;
    }
}
