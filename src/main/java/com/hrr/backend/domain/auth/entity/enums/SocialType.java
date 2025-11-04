package com.hrr.backend.domain.auth.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

public enum SocialType {
    KAKAO("kakao"),
    NAVER("naver"),
    APPLE("apple");

    private final String value;

    SocialType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SocialType from(String value) {
        for (SocialType type : SocialType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new GlobalException(ErrorCode.AUTH_INVALID_SOCIAL_TYPE);
    }
}
