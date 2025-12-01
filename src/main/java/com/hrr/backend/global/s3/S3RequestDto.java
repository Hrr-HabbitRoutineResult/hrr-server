package com.hrr.backend.global.s3;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class S3RequestDto {

	@Getter
	@NoArgsConstructor
	@Schema(description = "이미지 업로드 url 발급 요청 DTO")
	public static class PostDto{

		@NotBlank(message = "파일 이름은 필수 입력 항목입니다.")
		private String fileName;
	}
}
