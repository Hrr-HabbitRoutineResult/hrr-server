package com.hrr.backend.global.s3;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "S3", description = "S3 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {
	private final S3Service s3Service;

	@PostMapping("/presigned-url")
	public ApiResponse<S3ResponseDto.PostDto> getPresignedUrl(
		@RequestBody S3RequestDto.PostDto request
	) {

		S3ResponseDto.PostDto response = s3Service.getPresignedUrl(
			request.getFileName()
		);

		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}
}
