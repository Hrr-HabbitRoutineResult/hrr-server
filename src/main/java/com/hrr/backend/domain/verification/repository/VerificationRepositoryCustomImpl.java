package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.round.entity.QRoundRecord;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.QVerification;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VerificationRepositoryCustomImpl implements VerificationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<Verification> findVerificationHistoryByUser(User user, Pageable pageable) {
        QVerification qVerification = QVerification.verification;
        QRoundRecord qRoundRecord = QRoundRecord.roundRecord;
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;
        QChallenge qChallenge = QChallenge.challenge;

        // 엔티티 조회 (size + 1로 hasNext 판단)
        List<Verification> content = jpaQueryFactory
                .select(qVerification)
                .from(qVerification)
                .join(qVerification.roundRecord, qRoundRecord).fetchJoin()
                .join(qRoundRecord.userChallenge, qUserChallenge).fetchJoin()
                .join(qUserChallenge.challenge, qChallenge).fetchJoin()
                .where(
                        qUserChallenge.user.eq(user),
                        qVerification.status.eq(VerificationStatus.COMPLETED)
                )
                .orderBy(qVerification.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)  // hasNext 판단을 위해 +1
                .fetch();

        // Slice 생성 (hasNext 판단)
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}