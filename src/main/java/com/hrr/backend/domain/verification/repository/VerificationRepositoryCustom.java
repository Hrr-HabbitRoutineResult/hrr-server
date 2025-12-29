package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface VerificationRepositoryCustom {

    /**
     * 사용자의 전체 챌린지 인증 기록 조회 (페이징)
     * @param user 조회 대상 사용자
     * @param pageable 페이징 정보
     * @return 인증 엔티티 Slice
     */
    Slice<Verification> findVerificationHistoryByUser(User user, Pageable pageable);
}