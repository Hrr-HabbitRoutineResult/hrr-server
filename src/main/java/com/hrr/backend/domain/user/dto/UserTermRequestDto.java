package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UserTermRequestDto {

    @Schema(description = "동의한 약관 ID 목록")
    public record AgreeRequest(
            @NotEmpty(message = "agreedTermIds는 비어 있을 수 없습니다.")
            List<Long> agreedTermIds
    ) {}
}
