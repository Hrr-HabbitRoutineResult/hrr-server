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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4041", "존재하지 않는 유저입니다."),
    NICKNAME_BLANK(HttpStatus.BAD_REQUEST, "USER4002", "닉네임을 입력해 주세요."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "USER4003", "닉네임은 최대 10자까지 입력 가능합니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER4094", "해당 닉네임은 이미 등록되어 있어요!"),
    INVALID_LOGIN_STATUS_FOR_NICKNAME(HttpStatus.BAD_REQUEST, "USER4005", "닉네임 설정은 약관 동의 후에만 가능합니다."),


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
    EMBEDDING_LENGTH_ERROR(HttpStatus.BAD_REQUEST, "CHALLENGE4009", "임베딩 길이가 올바르지 않습니다. (expected: 768)"),
    CHALLENGE_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "CHALLENGE40010", "진행 중인 챌린지가 아닙니다."), // feat/90
    USER_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGE40410", "해당 챌린지에 참가하지 않았습니다."), // develop
    CHALLENGE_INVALID_START_DATE(HttpStatus.BAD_REQUEST, "CHALLENGE40011", "챌린지 시작 날짜는 내일 이후여야 합니다."), // develop

    // challenge wait
    CHALLENGE_WAIT_ALREADY_EXIST(HttpStatus.CONFLICT, "WAIT4091", "이미 알림 신청을 완료한 챌린지입니다."),
    CHALLENGE_WAIT_NOT_FOUND(HttpStatus.NOT_FOUND, "WAIT4041", "알림 신청 내역을 찾을 수 없습니다."),
    CHALLENGE_NOT_FULL(HttpStatus.BAD_REQUEST, "WAIT4001", "아직 모집 인원이 마감되지 않아 바로 참여 가능합니다."),

    // auth
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH001", "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH002", "토큰이 만료되었습니다."),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH003", "해당 사용자를 찾을 수 없습니다."),
    AUTH_INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH004", "지원하지 않는 소셜 로그인 타입입니다."),
    AUTH_UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH005", "현재는 Kakao 로그인만 지원합니다."),
    AUTH_EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH006", "외부 인증 서버와 통신 중 오류가 발생했습니다."),
    AUTH_KAKAO_TOKEN_ERROR(HttpStatus.BAD_GATEWAY, "AUTH007", "카카오 토큰 요청 중 오류가 발생했습니다."),
    AUTH_KAKAO_USER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH008", "카카오 사용자 정보 조회 중 오류가 발생했습니다."),
    AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH4019", "Authorization 헤더가 필요합니다."),
    AUTH_TOKEN_INVALIDATED(HttpStatus.UNAUTHORIZED, "AUTH4011", "로그아웃되어 만료된 토큰입니다."),

    // round
    ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUND4041", "라운드를 찾을 수 없습니다."), // feat/90 & develop 공통
    ROUND_NOT_MATCH_CHALLENGE(HttpStatus.BAD_REQUEST, "ROUND4002", "라운드가 해당 챌린지에 속하지 않습니다."),
    ROUND_NOT_STARTED(HttpStatus.BAD_REQUEST, "ROUND4003", "아직 시작되지 않은 라운드입니다."),
    ROUND_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "ROUND4004", "이미 종료된 라운드입니다."),
    ROUND_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUND4045", "라운드 기록을 찾을 수 없습니다."),

    // mission
    RANDOM_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION404", "미션을 찾을 수 없습니다."),

    // term
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM4041", "존재하지 않는 약관입니다."),
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERM4002", "필수 약관에 모두 동의해야 합니다."),
    TERM_ALREADY_AGREED(HttpStatus.BAD_REQUEST, "TERM4003", "이미 약관 동의를 완료한 사용자입니다."),
    INVALID_TERM_ID_IN_REQUEST(HttpStatus.BAD_REQUEST, "TERM4004", "요청에 유효하지 않은 약관 ID가 포함되어 있습니다."),
    TERM_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TERM5005", "약관 동의 저장 중 오류가 발생했습니다."),

    // verification
    VERIFICATION_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "VERIFICATION4001", "글 인증에서는 내용이 반드시 필요합니다."),
    VERIFICATION_URL_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION4002", "잘못된 URL 형식입니다."),
    VERIFICATION_NOT_TEXT_TYPE(HttpStatus.BAD_REQUEST, "VERIFICATION4003", "이 챌린지는 글 인증 방식이 아닙니다."),
    VERIFICATION_USER_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFICATION4044", "챌린지 참가 이력이 없습니다."),
    VERIFICATION_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFICATION4045", "해당 라운드를 찾을 수 없습니다."),
    VERIFICATION_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "VERIFICATION4006", "사진 인증에서는 파일이 반드시 필요합니다."),
    VERIFICATION_FILE_NOT_IMAGE(HttpStatus.BAD_REQUEST, "VERIFICATION4007", "이미지 파일만 업로드할 수 있습니다."),
    VERIFICATION_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "VERIFICATION4008", "제목은 필수 입력 값입니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFICATION4049", "해당 인증글을 찾을 수 없습니다."),
    VERIFICATION_ROUND_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION40010", "유효하지 않은 라운드입니다."),
    VERIFICATION_FILTER_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION40011", "조회 필터 조건이 올바르지 않습니다."),
    VERIFICATION_FILE_EMPTY(HttpStatus.BAD_REQUEST, "VERIFICATION40012", "파일이 비어있습니다."),
    VERIFICATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "VERIFICATION40013", "이미 인증을 완료했습니다."),
    VERIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "VERIFICATION40314", "인증에 대한 접근 권한이 없습니다."),
    VERIFICATION_NOT_QUESTION(HttpStatus.BAD_REQUEST, "VERIFICATION40015", "질문 인증글이 아니어서 댓글을 채택할 수 없습니다."),
    VERIFICATION_ALREADY_RESOLVED(HttpStatus.CONFLICT, "VERIFICATION40916", "이미 해결된 인증글입니다."),

    // file upload
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE5001", "파일 업로드에 실패했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE4002", "파일 크기가 제한을 초과했습니다."),
    FILE_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "FILE4003", "지원하지 않는 파일 형식입니다."),

    // comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT4041", "존재하지 않는 댓글입니다."),
    COMMENT_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "COMMENT4002", "댓글 내용은 비어 있을 수 없습니다."),
    COMMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "COMMENT4033", "본인이 작성한 댓글만 수정/삭제할 수 있습니다."),
    COMMENT_INVALID_PARENT(HttpStatus.BAD_REQUEST, "COMMENT4004", "유효하지 않은 부모 댓글입니다."),
    COMMENT_DEPTH_INVALID(HttpStatus.BAD_REQUEST, "COMMENT4005", "대댓글의 depth 값이 올바르지 않습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT4006", "대댓글에는 답글을 달 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT4037", "댓글에 접근할 수 없습니다."),
    COMMENT_INVALID(HttpStatus.BAD_REQUEST, "COMMENT4008", "유효하지 않은 댓글입니다."),

    // follow
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "FOLLOW4001", "자기 자신을 팔로우할 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "FOLLOW4091", "이미 팔로우 중인 사용자입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW4041", "팔로우 관계를 찾을 수 없습니다."),

    // search
    MIGRATION_REDIS_TO_DB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SEARCH5001", "redis에서 DB로 로그를 저장하는 데 실패했습니다."),
    COUNTING_LOG_TABLE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SEARCH5002", "집계가 실패하였습니다."),
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