package com.hrr.backend.domain.challenge.dto;

import com.hrr.backend.global.common.enums.ChallengeDays;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;

public class ChallengeResponseDto {

	@Setter
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
	}


}
