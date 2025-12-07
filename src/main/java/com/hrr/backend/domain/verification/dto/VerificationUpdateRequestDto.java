package com.hrr.backend.domain.verification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationUpdateRequestDto {

    private String title;
    private String content;
    private String textUrl;
    private String photoUrl;
}
