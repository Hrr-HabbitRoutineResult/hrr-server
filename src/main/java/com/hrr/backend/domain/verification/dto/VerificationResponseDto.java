package com.hrr.backend.domain.verification.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.response.SliceResponseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VerificationResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 피드(목록) 항목 DTO")
    public static class FeedDto {

        @Schema(description = "인증 ID", example = "10")
        private Long verificationId;

        @Schema(description = "인증 유형 (CAMERA: 사진, TEXT: 글)", example = "TEXT")
        private VerificationPostType type;

        @Schema(description = "제목", example = "오늘의 질문입니다!")
        private String title;

        @Schema(description = "내용 (글 인증 미리보기용)", example = "이 부분 어떻게 해결하나요?")
        private String content;

        @Schema(description = "인증 사진 URL (사진 인증인 경우)", example = "https://example.com/photo.jpg")
        private String imageUrl;

        @Schema(description = "외부 링크 포함 여부 (true: 링크 있음, false: 없음)", example = "true")
        private Boolean hasLink;

        @Schema(description = "질문글 여부 (Q 마크 표시)", example = "true")
        private Boolean isQuestion;

        @Schema(description = "질문 해결 여부 (채택 완료됨 - 정렬 후순위)", example = "false")
        private Boolean isResolved;

        @Schema(description = "작성자 닉네임", example = "코딩하는고양이")
        private String writerNickname;

        @Schema(description = "작성자 프로필 URL", example = "https://example.com/profile.jpg")
        private String writerProfileUrl;
        
        @Schema(description = "작성자 ID (본인 글 확인용)", example = "1")
        private Long writerId;

        @Schema(description = "작성일자", example = "2025.12.05")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        private LocalDateTime createdDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "인증 통계 정보 DTO (헤더용)")
    public static class StatDto {

        @Schema(description = "집계된 인증 인원 수 (분자)", example = "15")
        private Integer certifiedCount;

        @Schema(description = "전체 참가자 수 (분모)", example = "30")
        private Integer totalParticipantCount;

        @Schema(description = "집계 기준 날짜 (null이면 인증 내역 없음)", example = "2025-12-05")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        private LocalDateTime baseDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "내 인증 현황 프로필 DTO")
    public static class MyProfileDto {

        @Schema(description = "사용자 닉네임", example = "해빗")
        private String nickname;

        @Schema(description = "누적 인증 횟수 (해당 챌린지 내 전체)", example = "15")
        private Long totalVerificationCount;

        @Schema(description = "누적 경고 횟수", example = "1")
        private Integer warningCount;

        @Schema(description = "현재 참여 중인 라운드 순서 (예: 1회차, 2회차...)", example = "3")
        private Long currentRoundSequence;

        @Schema(description = "내 인증글 목록 (페이징)")
        private SliceResponseDto<FeedDto> verifications;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증글 생성/상세 응답 DTO")
    public static class CreateResponseDto {

        private Long verificationId;
        private Long roundId;
        private Long challengeId;
        private Long userChallengeId;

        @Schema(description = "인증 유형 (CAMERA / TEXT)")
        private VerificationPostType type;

        private String title;
        private String content;
        private String photoUrl;
        private String textUrl;
        private List<String> textImages;
        private Boolean isQuestion;

        @Schema(description = "상태 (TEMPORARY / COMPLETED)")
        private VerificationStatus status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        private Long userId;
        private String userNickname;

        /** 채택 완료 여부 (develop) */
        private Boolean isAdopted; // 참고: Entity 병합 시 isAdopted는 Comment로 이동했으나, develop DTO 요구사항 유지를 위해 남김 (필요 시 제거)
        
        /** 현재 라운드 인증 횟수 */
        private Integer verificationCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 전체 인증 기록 DTO")
    public static class HistoryDto {

        @Schema(description = "인증 ID", example = "1")
        private Long verificationId;

        @Schema(description = "챌린지 ID", example = "101")
        private Long challengeId;

        @Schema(description = "챌린지 제목", example = "미라클 모닝")
        private String challengeTitle;

        @Schema(description = "인증 타입", example = "TEXT", allowableValues = {"TEXT", "CAMERA"})
        private String type;

        @Schema(description = "인증 제목", example = "해피뉴이어! 올해 마지막 인증 올립니다")
        private String title;

        @Schema(description = "인증 내용", example = "여기엔 상세내용이 들어가유~")
        private String content;

        @Schema(description = "사진 URL (사진 인증)", example = "https://example.com/verification_image_1.jpg")
        private String photoUrl;

        @Schema(description = "글 URL (글 인증)", example = "https://blog.example.com/post/123")
        private String textUrl;

        private List<String> textImages;

        @Schema(description = "인증 일시", example = "2025-09-18T08:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "다른 사용자 인증 기록 조회 응답 DTO")
    public static class OtherUserHistoryResponse {

        @Schema(description = "프로필 공개 여부", example = "true")
        private Boolean isPublic;

        @Schema(description = "사용자 닉네임", example = "testuser")
        private String nickname;

        @Schema(description = "인증 기록 리스트 (비공개시 빈 배열)")
        private SliceResponseDto<HistoryDto> verifications;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 게시글 스크랩 등록 응답 DTO")
    public static class ScrapResponseDto {

        @Schema(description = "인증 게시글 ID", example = "125")
        private Long verificationId;

        @Schema(description = "스크랩 여부", example = "true")
        private Boolean isScrapped;
    }
}
