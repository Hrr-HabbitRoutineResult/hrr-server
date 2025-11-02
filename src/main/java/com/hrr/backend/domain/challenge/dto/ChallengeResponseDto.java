package com.hrr.backend.domain.challenge.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hrr.backend.global.common.enums.ChallengeDays;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

		@Schema(description = "챌린지 인증 요일 (MONDAY, TUESDAY 등)", example = "MONDAY")
		private ChallengeDays daysOfWeek;

		@Schema(description = "챌린지 대표 이미지 URL", example = "http://example.com/images/thumb.jpg")
		private String thumbnailUrl;

		@JsonIgnore
		private LocalDateTime startDate;	// Querydsl로 조회하고 Service에서 사용 후 버릴 필드라 ignore 처리
	}


}
