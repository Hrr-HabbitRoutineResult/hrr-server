package com.hrr.backend.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "status", "code", "message", "result"})
public class ApiResponse<T> {

	@JsonProperty("isSuccess")
	private final Boolean isSuccess;
	private final String status;  // e.g. OK, BAD_REQUEST 등
	private final String code;
	private final String message;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T result;


	// 성공 응답 생성
	public static <T> ApiResponse<T> onSuccess(SuccessCode code, T data) {
		return new ApiResponse<>(true, code.getHttpStatus().name(), code.getCode(), code.getMessage(), data);
	}

	// 실패 응답 생성
	public static <T> ApiResponse<T> onFailure(ErrorCode code, T data) {
		return new ApiResponse<>(false, code.getHttpStatus().name(), code.getCode(), code.getMessage(), data);
	}
}
