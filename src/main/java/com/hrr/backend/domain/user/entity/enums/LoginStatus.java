package com.hrr.backend.domain.user.entity.enums;

/**
 * LoginStatus는 사용자가 온보딩에서 어느 단계에 있는지 표현하기 위함임
 *
 * NEW: 소셜 로그인만 완료 (약관 동의 필요)
 * TERMS_DONE: 약관 동의 완료 (가입정보 입력 필요)
 * ACTIVE: 온보딩 완료(기존 유저, 메인 진입 가능)
 */
public enum LoginStatus {
    NEW("NEW"),
    TERMS_DONE("TERMS_DONE"),
    ACTIVE("ACTIVE");

    private final String value;

    LoginStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
