package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundDropServiceImpl implements RoundDropService {

    private final RoundRepository roundRepository;
    private final RoundDropProcessor roundDropProcessor;

    @Override
    public void dropNonContinuersAt(LocalDate endDate) {

        // 오늘이 endDate인 라운드들 => 오늘 23:59에 드랍 처리 대상
        List<Round> rounds = roundRepository.findAllByEndDate(endDate);

        // 각 Round는 독립 트랜잭션으로 처리
        // 한 Round 처리 중 예외가 발생해도 로그만 남기고 다음 Round 처리를 계속 진행
        for (Round round : rounds) {
            try {
                roundDropProcessor.processRound(round);
            } catch (Exception e) {
                log.error("[RoundDrop] 드랍 처리 실패. roundId={}", round.getId(), e);
            }
        }
    }
}
