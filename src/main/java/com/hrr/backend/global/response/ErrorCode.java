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
	DM_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "DM4041", "존재하지 않는 대화방입니다."),
	DM_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "DM4042", "존재하지 않는 대화입니다."),

	// challenge
	CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGE4041", "존재하지 않는 챌린지입니다."),
	CHALLENGE_INVALID_VERIFY_TIME(HttpStatus.BAD_REQUEST, "CHALLENGE4001", "인증 종료 시간은 시작 시간보다 늦어야 합니다."),
	CHALLENGE_PRIVATE_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "CHALLENGE4002", "비공개 챌린지는 4자리 숫자 비밀번호가 필요합니다."),
	CHALLENGE_PRIVATE_VIEWER_MODE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHALLENGE4003", "비공개 챌린지는 관찰자 모드를 사용할 수 없습니다."),
	CHALLENGE_INVALID_MAX_PARTICIPANTS(HttpStatus.BAD_REQUEST, "CHALLENGE4004", "참가자 수는 1명 이상이어야 합니다."),
	CHALLENGE_PUBLIC_PASSWORD_INPUT(HttpStatus.BAD_REQUEST, "CHALLENGE4005", "공개 챌린지는 비밀번호를 설정할 수 없습니다."),
	CHALLENGE_ALREADY_JOINED(HttpStatus.CONFLICT, "CHALLENGE4091", "이미 참가한 챌린지입니다."),
	CHALLENGE_FULL(HttpStatus.CONFLICT, "CHALLENGE4092", "챌린지 정원이 초과되었습니다."),
	CHALLENGE_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "CHALLENGE4006", "비밀번호가 일치하지 않습니다."),
	CHALLENGE_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "CHALLENGE4007", "모집 중인 챌린지가 아닙니다."),
    CHALLENGE_CALCULATE_EMBEDDING(HttpStatus.BAD_REQUEST, "CHALLENGE4006", "챌린지 임베딩 계산 중 오류가 발생했습니다."),
    EMBEDDING_API_ERROR(HttpStatus.BAD_REQUEST, "CHALLENGE4007", "임베딩 API로부터 유효하지 않은 응답을 받았습니다."),
    EMBEDDING_INVALID_INPUT(HttpStatus.BAD_REQUEST, "CHALLENGE4008", "임베딩을 위한 텍스트는 필수입니다."),

	// auth
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH001", "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH002", "토큰이 만료되었습니다."),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH003", "해당 사용자를 찾을 수 없습니다."),
    AUTH_INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH004", "지원하지 않는 소셜 로그인 타입입니다."),
    AUTH_UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH005", "현재는 Kakao 로그인만 지원합니다."),
    AUTH_EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH006", "외부 인증 서버와 통신 중 오류가 발생했습니다."),
    AUTH_KAKAO_TOKEN_ERROR(HttpStatus.BAD_GATEWAY, "AUTH007", "카카오 토큰 요청 중 오류가 발생했습니다."),
    AUTH_KAKAO_USER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH008", "카카오 사용자 정보 조회 중 오류가 발생했습니다."),

	// mission
	RANDOM_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "미션을 찾을 수 없습니다.")

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

