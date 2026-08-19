package com.hrr.backend.domain.report.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.report.entity.ChallengeReport;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeReportRepository extends JpaRepository<ChallengeReport, Long> {

    // 중복 신고 확인
    boolean existsByReporterAndChallenge(User reporter, Challenge challenge);
}
