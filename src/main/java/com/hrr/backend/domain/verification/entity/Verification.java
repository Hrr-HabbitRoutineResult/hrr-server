package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.round.entity.RoundRecord; // [중요] RoundRecord Import
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_record_id", nullable = false)
    private RoundRecord roundRecord;

    @Enumerated(EnumType.STRING)
    private VerificationPostType type; // CAMERA, TEXT

    private String title;

    @Lob
    private String content;

    private String photoUrl;

    private String textUrl;

    private Boolean isQuestion;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // TEMPORARY, COMPLETED

    private Long roundId;

    /** 유저 챌린지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_challenge_id")
    private UserChallenge userChallenge;


    public static Verification createTextVerification(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            String title,
            String content,
            String textUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .roundRecord(roundRecord)
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

    public static Verification createPhotoVerification(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            String title,
            String photoUrl,
            Boolean isQuestion,
            Long roundId
    ) {
        return Verification.builder()
                .roundRecord(roundRecord)
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