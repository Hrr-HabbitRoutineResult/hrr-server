package com.hrr.backend.global.response;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode implements BaseCode{

	// temp
	TEST_ERROR(HttpStatus.BAD_REQUEST, "TEST", "오류 응답에 대한 테스트입니다."),

	// command
	_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
	_BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),

	// user
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "존재하지 않는 유저입니다."),

	// fcm
	FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM404", "존재하지 않는 FCM 토큰입니다."),

	// dm
	DM_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "DM404", "존재하지 않는 대화방입니다."),
	DM_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "DM404", "존재하지 않는 대화입니다."),

    // auth
    AUTH_INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH400", "지원하지 않는 소셜 로그인 타입입니다."),
    AUTH_UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH401", "현재는 Kakao 로그인만 지원합니다."),
    AUTH_EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_004", "외부 인증 서버와 통신 중 오류가 발생했습니다."),


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

