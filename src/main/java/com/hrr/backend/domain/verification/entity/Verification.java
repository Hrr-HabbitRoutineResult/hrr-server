package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.round.entity.RoundRecord; // [중요] RoundRecord Import
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
@Table(
        name = "verification",
        indexes = {
                // 날짜 범위 검색 최적화
                @Index(name = "idx_verification_created_at", columnList = "createdAt"),
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

    public static Verification createTextVerification(
            RoundRecord roundRecord,
            String title,
            String content,
            String textUrl,
            Boolean isQuestion
    ) {
        return Verification.builder()
                .roundRecord(roundRecord)
                .type(VerificationPostType.TEXT)
                .title(title)
                .content(content)
                .textUrl(textUrl)
                .isQuestion(isQuestion)
                .status(VerificationStatus.TEMPORARY)
                .build();
    }

    public static Verification createPhotoVerification(
            RoundRecord roundRecord,
            String title,
            String photoUrl,
            Boolean isQuestion
    ) {
        return Verification.builder()
                .roundRecord(roundRecord)
                .type(VerificationPostType.CAMERA)
                .title(title)
                .photoUrl(photoUrl)
                .isQuestion(isQuestion)
                .status(VerificationStatus.TEMPORARY)
                .build();
    }
}