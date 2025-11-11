package com.hrr.backend.domain.user.entity.enums;

/*LoginStatus는 사용자의 상태(활성, 휴면, 정지)를 나타내는게 아니라
* 추후 Onboarding을 위해서 신규가입인지 기존 유저인지를 나타내기 위함임. */
public enum LoginStatus {
    NEW("new"),
    EXISTING("existing");

    private final String value;

    LoginStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}