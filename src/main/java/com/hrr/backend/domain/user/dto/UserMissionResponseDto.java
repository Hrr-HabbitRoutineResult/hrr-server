package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserMissionResponseDto {

	@Setter
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "오늘의 랜덤미션 상세정보 DTO")
	public static class DetailDto {

		@Schema(description = "미션 아이디", example = "101")
		private Long missionId;

		@Schema(description = "미션 제목", example = "오운완")
		private String title;

		@Schema(description = "미션 내용", example = "운동 앱에 저장된 내 운동 내역을 인증해요.")
		private String content;

		@Schema(description = "완료 여부", example = "true")
		private Boolean isCompleted;

	}
}
