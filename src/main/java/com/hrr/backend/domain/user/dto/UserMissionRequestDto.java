package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserMissionRequestDto {

	@Getter
	public static class VerificationDto{
		@NotNull(message = "미션 ID는 필수입니다")
		@Schema(description = "missionid를 입력해주세요.", example = "1")
		private Long missionId;

		@Schema(description = "인증 이미지 S3 Key", example = "uploads/uuid_image.jpg")
		@NotBlank(message = "인증 이미지는 필수입니다.")
		private String imageKey;
	}
}
