package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.QVerification;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.round.entity.QRoundRecord;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.QUser;
import com.hrr.backend.global.util.UserFilterExpressions;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class VerificationRepositoryImpl implements VerificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Verification> findVerificationFeed(
            Long roundId,
            VerificationPostType type,
            VerificationStatus status,
            Long currentUserId,
            Pageable pageable
    ) {
        QVerification v = QVerification.verification;
        QRoundRecord r = QRoundRecord.roundRecord;
        QUserChallenge uc = QUserChallenge.userChallenge;
        QUser u = QUser.user;

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);

        // 1. 데이터 조회 쿼리
        List<Verification> content = queryFactory
                .selectFrom(v)
                .join(v.roundRecord, r).fetchJoin()
                .join(r.userChallenge, uc).fetchJoin()
                .join(uc.user, u).fetchJoin()
                .where(
                        r.round.id.eq(roundId),
                        v.type.eq(type),
                        v.status.eq(status),
                        // 내가 차단한 유저만 제외
                        UserFilterExpressions.notBlockedByMe(currentUserId)
                )
                .orderBy(
                        // 미해결 질문 우선 정렬
                        new CaseBuilder()
                                .when(
                                        v.isQuestion.isTrue()
                                                .and(v.isResolved.isFalse())
                                                .and(v.createdAt.after(oneWeekAgo)) // 7일 이내 조건 추가
                                ).then(0)
                                .otherwise(1).asc(),
                        v.createdAt.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리
        Long total = queryFactory
                .select(v.count())
                .from(v)
                .join(v.roundRecord, r)
                .join(r.userChallenge, uc)
                .join(uc.user, u)
                .where(
                        r.round.id.eq(roundId),
                        v.type.eq(type),
                        v.status.eq(status),
                        // 동일하게 내가 차단한 유저만 제외
                        UserFilterExpressions.notBlockedByMe(currentUserId)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}