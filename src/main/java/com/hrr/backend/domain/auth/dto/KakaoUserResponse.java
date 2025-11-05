package com.hrr.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoUserResponse {
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {
        private Profile profile;
        private String email; // 선택 동의 항목 (없으면 null)

        @Data
        public static class Profile {
            private String nickname;
            @JsonProperty("profile_image_url")
            private String profile_image_url;
        }
    }
}
