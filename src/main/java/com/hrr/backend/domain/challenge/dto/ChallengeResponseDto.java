package com.hrr.backend.domain.challenge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.entity.enums.ExtensionStatus;
import com.hrr.backend.global.common.enums.ChallengeDays;

import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.common.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class ChallengeResponseDto {

	@Setter
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "챌린지 기본 정보 DTO")
	public static class InfoDto {

		@Schema(description = "챌린지 아이디", example = "101")
		private Long challengeId;

		@Schema(description = "챌린지 제목", example = "매일 아침 6시 기상 챌린지")
		private String title;

		@Schema(description = "챌린지 간단 설명", example = "건강한 아침을 위한 습관 만들기!")
		private String description;

		@Schema(description = "현재 참여 인원 수", example = "5")
		private Integer currentParticipantCount;

		@Schema(description = "최대 참여 가능 인원 수", example = "10")
		private Integer maxParticipantCount;

		@Schema(description = "곧 시작할 챌린지 여부 (5일 이내)", example = "true")
		private Boolean isUpcoming;

		@Schema(description = "챌린지 시작까지 남은 일수 (D-Day=0, 미래만 양수)", example = "3")
		private Integer dDayUntilStart;

		@Schema(description = "챌린지 인증 요일 목록", example = "['MONDAY', 'WEDNESDAY']")
		private List<ChallengeDays> daysOfWeek;

		@Schema(description = "챌린지 대표 이미지 URL", example = "http://example.com/images/thumb.jpg")
		private String thumbnailUrl;

		@JsonIgnore
		private LocalDateTime startDate;	// Querydsl로 조회하고 Service에서 사용 후 버릴 필드라 ignore 처리
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "오늘의 인기 챌린지 DTO")
	public static class DailyTopDto {

		@Setter
		@Schema(description = "랭킹", example = "1")
		private Integer ranking;

		@Schema(description = "클릭 수", example = "1500")
		private Long clickCount;

		@Schema(description = "챌린지 정보")
		private InfoDto info;

	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "챌린지 개설 응답 DTO")
	public static class CreateChallengeDto {

		@Schema(description = "생성된 챌린지 ID", example = "1")
		private Long id;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "챌린지 참가 응답 DTO")
	public static class JoinChallengeDto {

		@Schema(description = "참가한 챌린지 ID", example = "1")
		private Long challengeId;

		@Schema(description = "참가 후 현재 인원 수", example = "6") // 추가
		private Integer currentParticipantCount;
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "챌린지 좋아요 등록/취소 응답 DTO")
	public static class ChallengeLikeDto {

		@Schema(description = "대상 챌린지 ID", example = "1")
		private Long challengeId;

		@Schema(description = "좋아요 상태 여부 (true: 좋아요 등록됨, false: 좋아요 취소됨)", example = "true")
		private Boolean isLiked;

		@Schema(description = "갱신된 챌린지의 총 좋아요 수", example = "15")
		private Integer likeCount;
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "챌린지 상세 상단 정보 응답 DTO")
	public static class HeaderInfoDto {

		@Schema(description = "챌린지 ID", example = "101")
		private Long challengeId;

		@Schema(description = "챌린지 제목", example = "백준 실버3 코테")
		private String title;

		@Schema(description = "챌린지 설명 (부제목)", example = "백준 실버3 매일 풀고 공유")
		private String description;

		@Schema(description = "인증 방법 (PHOTO: 사진, TEXT: 글)", example = "PHOTO")
		private VerificationType verificationType;

		@Schema(description = "챌린지 배경 이미지 URL", example = "https://example.com/image.jpg")
		private String imageUrl;

		@Schema(description = "현재 참여 인원", example = "10")
		private Integer currentParticipantCount;

		@Schema(description = "최대 참여 인원", example = "30")
		private Integer maxParticipantCount;

		@Schema(description = "챌린지 시작일", example = "2025-10-01")
		private LocalDate startDate;

		@Schema(description = "챌린지 종료일", example = "2025-10-31")
		private LocalDate endDate;

		@Schema(description = "종료까지 남은 일수 (D-Day)", example = "15")
		private Long remainDays;

		@Schema(description = "챌린지 공개 여부", example = "true")
		private Boolean isPublic;

		// 상태 플래그
		@Schema(description = "관찰자 모드 여부", example = "true")
		private Boolean isObserverMode;

		@Schema(description = "현재 유저의 참여 여부", example = "true")
		private Boolean isParticipant;

        @Schema(description = "현재 유저가 방장인지 여부", example = "false")
        private Boolean isOwner;

		@Schema(description = "현재 유저의 좋아요(찜) 여부", example = "false")
		private Boolean isLiked;

		// 하단 버튼 상태
		@Schema(description = "하단 버튼 상태 (JOIN, DISABLED, WAITLIST, CERTIFY_AVAILABLE, CERTIFIED)", example = "CERTIFIED")
		private ActionButtonStatus actionButtonStatus;

		// 방장 정보
		@Schema(description = "방장 정보")
		private OwnerDto owner;
	}

	// 방장 정보 DTO 내부 클래스
	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "방장 정보 DTO")
	public static class OwnerDto {
		@Schema(description = "방장 유저 ID", example = "1")
		private Long id;

		@Schema(description = "방장 닉네임", example = "김호르")
		private String nickname;

		@Schema(description = "방장 프로필 이미지 URL", example = "https://example.com/profile.jpg")
		private String profileImageUrl;
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "챌린지 프로필(메인 화면) 응답 DTO - Flat Structure")
	public static class ChallengeProfileDto {

		@Schema(description = "챌린지 ID", example = "1")
		private Long challengeId;

		@Schema(description = "참여 여부 (true: 2번 UI / false: 1번 UI)", example = "true")
		private Boolean isParticipating;

		@Schema(description = "챌린지 규칙", example = "하루에 1만 보 이상 걸은 스크린샷을 인증해야 합니다.")
		private String rule;

		@Schema(description = "목표 요일 목록", example = "[\"MONDAY\", \"THURSDAY\"]")
		private List<ChallengeDays> targetDays;

		@Schema(description = "인증 가능 시작 시간", example = "10:00:00")
		private LocalTime verifyStartTime;

		@Schema(description = "인증 가능 종료 시간", example = "18:00:00")
		private LocalTime verifyEndTime;

		@Schema(description = "이번 주 인증 완료 요일 (참여 중일 때만 값 있음, 미참여시 null)", example = "[\"MONDAY\"]")
		private List<ChallengeDays> verifiedDaysThisWeek;

		@Schema(description = "연장 요청 상태 (NONE, PENDING, COMPLETED)", example = "PENDING")
		private ExtensionStatus extensionStatus;
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@Schema(description = "챌린지 라운드 목록 응답 DTO")
	public static class RoundDto {

		@Schema(description = "라운드 회차 (탭 UI 표시 및 식별용)", example = "1")
		private Integer roundNumber;

		@Schema(description = "현재 진행 중인 라운드인지 여부 (TRUE일 경우 UI 강조)", example = "true")
		private Boolean isCurrentRound;

		@Schema(description = "로그인한 사용자의 해당 라운드 기록 존재 여부", example = "true")
		private Boolean isParticipated;
	}

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "챌린지 수정 응답 DTO")
    public static class UpdateChallengeDto {

        @Schema(description = "수정된 챌린지 ID", example = "1")
        private Long challengeId;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "챌린지 나가기 응답 DTO")
    public static class LeaveChallengeDto {

        @Schema(description = "나간 챌린지 ID", example = "1")
        private Long challengeId;

        @Schema(description = "나간 후 현재 인원 수", example = "9")
        private Integer currentParticipantCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "챌린지 수정용 상세 정보 응답 DTO (개설 화면 폼 채우기용)")
    public static class EditInfoDto {

        @Schema(description = "챌린지 제목", example = "백준 실버3 코테")
        private String title;

        @Schema(description = "챌린지 설명", example = "백준 실버3 매일 풀고 공유")
        private String description;

        @Schema(description = "공개 여부 (true: 공개, false: 비공개)", example = "true")
        private Boolean isPublic;

        @Schema(description = "비공개 비밀번호 설정 여부 (true: 설정됨, false: 없음/공개 챌린지). 평문 비밀번호는 응답에 포함되지 않으며, 수정 시 비워두면 기존 비밀번호가 유지됩니다.", example = "true")
        private Boolean hasPassword;

        @Schema(description = "챌린지 카테고리", example = "STUDY")
        private Category category;

        @Schema(description = "인증 방식", example = "PHOTO")
        private VerificationType verificationType;

        @Schema(description = "챌린지 시작일", example = "2025-11-24")
        private LocalDate startDate;

        @Schema(description = "최대 참여 인원", example = "10")
        private Integer maxParticipants;

        @Schema(description = "관찰자 모드 허용 여부", example = "true")
        private Boolean isViewerMode;

        @Schema(description = "챌린지 규칙", example = "하루에 1만 보 이상 걸은 스크린샷을 인증해야 합니다.")
        private String rule;

        @Schema(description = "인증 시작 시간", example = "10:00:00")
        private LocalTime verifyStartTime;

        @Schema(description = "인증 종료 시간", example = "18:00:00")
        private LocalTime verifyEndTime;

        @Schema(description = "인증 요일 목록", example = "[\"MONDAY\", \"THURSDAY\"]")
        private List<ChallengeDays> daysOfWeek;

        @Schema(description = "챌린지 이미지 Key", example = "challenges/uuid_image.jpg")
        private String imageKey;

        @Schema(description = "챌린지 이미지 미리보기 URL", example = "https://example.com/challenges/uuid_image.jpg")
        private String imageUrl;
    }
}
