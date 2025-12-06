package com.hrr.backend.global.response;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum SuccessCode implements BaseCode {

	// common
	OK(HttpStatus.OK, "OK200", "요청에 성공하였습니다."),

	// challenge wait
	CHALLENGE_WAIT_REGISTER_OK(HttpStatus.OK, "WAIT2001", "챌린지 대기 신청이 완료되었습니다."),
	CHALLENGE_WAIT_CANCEL_OK(HttpStatus.OK, "WAIT2002", "챌린지 대기 신청이 취소되었습니다."),

    // follow
    FOLLOW_SUCCESS(HttpStatus.OK, "FOLLOW2001", "팔로우 성공"),
    UNFOLLOW_SUCCESS(HttpStatus.OK, "FOLLOW2002", "언팔로우 성공"),
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

