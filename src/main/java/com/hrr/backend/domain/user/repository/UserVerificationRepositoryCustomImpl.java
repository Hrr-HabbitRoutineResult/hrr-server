package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.round.entity.QRoundRecord;
import com.hrr.backend.domain.user.dto.UserVerificationResponseDto;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.QVerification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserVerificationRepositoryCustomImpl implements UserVerificationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<UserVerificationResponseDto.VerificationItemDto> findVerificationHistoryByUser(User user, Pageable pageable) {
        QVerification qVerification = QVerification.verification;
        QRoundRecord qRoundRecord = QRoundRecord.roundRecord;
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;
        QChallenge qChallenge = QChallenge.challenge;

        // 데이터 조회 (size + 1로 hasNext 판단)
        List<UserVerificationResponseDto.VerificationItemDto> content = jpaQueryFactory
                .select(Projections.fields(UserVerificationResponseDto.VerificationItemDto.class,
                        qVerification.id.as("verificationId"),
                        qChallenge.id.as("challengeId"),
                        qChallenge.title.as("challengeTitle"),
                        qVerification.type.stringValue().as("type"),
                        qVerification.title,
                        qVerification.content,
                        qVerification.photoUrl.as("imageUrl"),
                        qVerification.createdAt.as("verifiedAt")
                ))
                .from(qVerification)
                .join(qVerification.roundRecord, qRoundRecord)
                .join(qRoundRecord.userChallenge, qUserChallenge)
                .join(qUserChallenge.challenge, qChallenge)
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