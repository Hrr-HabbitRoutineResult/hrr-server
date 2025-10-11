package com.hrr.backend.response;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum SuccessCode implements BaseCode {

	OK(HttpStatus.OK, "OK200", "요청에 성공하였습니다."),
	;

	private final HttpStatus status;
	private final String code;
	private final String message;

	@Override
	public HttpStatus getHttpStatus() {
		return status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}

