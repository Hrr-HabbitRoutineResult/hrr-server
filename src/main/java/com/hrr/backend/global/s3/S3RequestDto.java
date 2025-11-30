package com.hrr.backend.global.s3;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class S3RequestDto {

	@Getter
	@NoArgsConstructor
	@Schema(description = "이미지 업로드 url 발급 요청 DTO")
	public static class PostDto{

		private String fileName;
	}
}
