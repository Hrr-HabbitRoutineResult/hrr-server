package com.hrr.backend.domain.verification.entity;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.converter.StringListConverter;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

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

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> textImages = new ArrayList<>();

    private Boolean isQuestion;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // TEMPORARY, COMPLETED, BLOCKED

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private Boolean isResolved = false;

	@ColumnDefault("0")
	@Column(nullable = false)
	@Builder.Default
	private Integer reportCount = 0; // 신고 누적 횟수

	public void addReport() {
		this.reportCount++;
		if (this.reportCount >= 5) {
			this.status = VerificationStatus.BLOCKED;
		}
	}

    /** TEXT 인증 생성 */
    public static Verification createTextVerification(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            String title,
            String content,
            String textUrl,
            List<String> textImages,
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
                .textImages(textImages != null ? textImages : new ArrayList<>()) // Null Safe
                .isQuestion(isQuestion)
                .status(VerificationStatus.COMPLETED)
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
                .status(VerificationStatus.COMPLETED)
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

    public void update(
            String title,
            String content,
            String textUrl,
            List<String> textImages,
            Boolean isQuestion
    ) {
        if (title != null) {
            if (title.isBlank()) {
                throw new GlobalException(ErrorCode.VERIFICATION_TITLE_REQUIRED);
            }
            this.title = title;
        }

        if (content != null) {
            if (content.isBlank()) {
                throw new GlobalException(ErrorCode.VERIFICATION_TEXT_REQUIRED);
            }
            this.content = content;
        }
        // 텍스트 URL 업데이트 (null 허용일 경우 로직에 따라 분기 처리 필요, 여기선 그대로 반영)
        this.textUrl = textUrl;

        // 이미지 리스트 업데이트: 들어온 리스트가 null이 아니면 그대로 덮어씀 (순서 보장)
        if (textImages != null) {
            this.textImages = textImages;
        }

        if (isQuestion != null) {
            this.isQuestion = isQuestion;
        }
    }
    private String normalizeOptionalImageKey(String key) {
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            throw new GlobalException(ErrorCode.VERIFICATION_INVALID_IMAGE_KEY);
        }
        if ("null".equalsIgnoreCase(trimmed)) {
            throw new GlobalException(ErrorCode.VERIFICATION_INVALID_IMAGE_KEY);
        }
        return trimmed;
    }

}
