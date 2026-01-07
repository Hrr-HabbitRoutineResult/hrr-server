package com.hrr.backend.domain.round.dto;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import jakarta.validation.constraints.NotNull;

public record RoundDecisionRequestDto(
        @NotNull NextRoundIntent intent,
        Long notificationId
) {}