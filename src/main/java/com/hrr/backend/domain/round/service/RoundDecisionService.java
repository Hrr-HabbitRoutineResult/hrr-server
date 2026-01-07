package com.hrr.backend.domain.round.service;

import com.hrr.backend.domain.round.dto.RoundDecisionRequestDto;
import com.hrr.backend.domain.round.dto.RoundDecisionResponseDto;

public interface RoundDecisionService {
    RoundDecisionResponseDto decideNextRound(Long userId, Long challengeId, RoundDecisionRequestDto request);
}
