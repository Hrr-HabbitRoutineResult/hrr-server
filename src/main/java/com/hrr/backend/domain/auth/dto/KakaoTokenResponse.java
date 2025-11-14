package com.hrr.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//카카오의 실제 응답 JSON은 언더바가 포함된다고 해서 매핑했습니다
@Data
public class KakaoTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("id_token")
    private String idToken;     // OIDC 켠 경우
    @JsonProperty("expires_in")
    private Long   expiresIn;
    @JsonProperty("scope")
    private String scope;
}
