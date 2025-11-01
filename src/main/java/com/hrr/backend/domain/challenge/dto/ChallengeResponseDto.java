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

		private Long challengeId;
		private String title;
		private String description;
		private Integer currentParticipantCount;
		private Integer maxParticipantCount;
		private Boolean isUpcoming;
		private Integer dDayUntilStart;
		private ChallengeDays daysOfWeek;
		private String thumbnailUrl;

		@JsonIgnore
		private LocalDateTime startDate;	// Querydsl로 조회하고 Service에서 사용 후 버릴 필드라 ignore 처리
	}


}
