package com.hrr.backend.response;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode implements BaseCode{

	// temp
	TEST_ERROR(HttpStatus.BAD_REQUEST, "TEST", "오류 응답에 대한 테스트입니다."),

	// command
	_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
	_BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),

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

