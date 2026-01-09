package com.hrr.backend.domain.challenge.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.challenge.service.ChallengeStaticsService;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeStaticsUpdater {

    private final ChallengeRepository challengeRepository;
    private final ChallengeStaticsService challengeStaticsService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateInNewTx(Long challengeId) {
        Challenge ch = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
        challengeStaticsService.updateChallengeStatics(ch);
    }
}
