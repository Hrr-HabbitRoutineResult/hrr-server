package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.enums.ChallengeStatus;
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
public class UserChallengeRepositoryCustomImpl implements UserChallengeRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<UserResponseDto.OngoingChallengeDto> findOngoingChallengesByUser(User user, Pageable pageable) {
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;
        QChallenge qChallenge = QChallenge.challenge;

        // 챌린지 기본 정보와 UserChallenge 정보 조회
        List<UserResponseDto.OngoingChallengeDto> content = jpaQueryFactory
                .select(Projections.fields(UserResponseDto.OngoingChallengeDto.class,
                        qChallenge.id.as("challengeId"),
                        qChallenge.title,
                        qChallenge.description,
                        qChallenge.imageUrl.as("thumbnailUrl"),
                        qUserChallenge.verificationCount.as("currentRound"),
                        qChallenge.startDate
                ))
                .from(qUserChallenge)
                .join(qUserChallenge.challenge, qChallenge)
                .where(
                        qUserChallenge.user.eq(user),
                        qChallenge.status.in(ChallengeStatus.UPCOMING, ChallengeStatus.ONGOING)
                )
                .orderBy(qChallenge.startDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)  // hasNext 확인을 위해 +1
                .fetch();

        // Slice 객체 생성 및 반환
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());  // 다음 페이지 확인용 데이터 제거
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}