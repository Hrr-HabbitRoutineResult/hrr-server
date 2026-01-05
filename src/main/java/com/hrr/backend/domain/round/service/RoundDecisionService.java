package com.hrr.backend.domain.round.service;

import com.hrr.backend.domain.round.dto.RoundDecisionRequestDto;

public interface RoundDecisionService {
    void decideNextRound(Long userId, Long challengeId, RoundDecisionRequestDto request);
}
