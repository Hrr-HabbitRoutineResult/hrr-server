package com.hrr.backend.global.s3;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class S3ResponseDto {

	@Builder
	@Getter
	@Schema(description = "이미지 업로드 url 발급 응답 DTO")
	public static class PostDto{

		@Schema(description = "업로드 url")
		private String presignedUrl;

		@Schema(description = "s3 파일 주소(DB 저장용)")
		private String s3Key;
	}
}
