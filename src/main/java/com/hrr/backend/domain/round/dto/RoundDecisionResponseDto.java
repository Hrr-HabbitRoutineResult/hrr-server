package com.hrr.backend.domain.round.dto;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import lombok.Builder;

@Builder
public record RoundDecisionResponseDto(
        Long roundId,
        NextRoundIntent intent,
        Boolean isResponded // 요청하신 true 반환 필드
) {
    public static RoundDecisionResponseDto of(Long roundId, NextRoundIntent intent) {
        return RoundDecisionResponseDto.builder()
                .roundId(roundId)
                .intent(intent)
                .isResponded(true) // 항상 true로 반환 (응답 완료했으므로)
                .build();
    }
}