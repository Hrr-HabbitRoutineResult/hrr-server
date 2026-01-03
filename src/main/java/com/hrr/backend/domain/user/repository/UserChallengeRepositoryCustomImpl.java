package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.challenge.entity.QChallengeLike;
import com.hrr.backend.domain.round.entity.QRound;
import com.hrr.backend.domain.round.entity.QRoundRecord;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
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
        QRound qRound = QRound.round;
        QRoundRecord qRoundRecord = QRoundRecord.roundRecord;

        // 챌린지 기본 정보와 UserChallenge 정보 조회
        List<UserResponseDto.OngoingChallengeDto> content = jpaQueryFactory
                .select(Projections.fields(UserResponseDto.OngoingChallengeDto.class,
                        qChallenge.id.as("challengeId"),
                        qChallenge.title,
                        qChallenge.description,
                        qChallenge.imageKey.as("thumbnailUrl"),
                        qRound.roundNumber.as("currentRound"),
                        qChallenge.startDate
                ))
                .from(qUserChallenge)
                .join(qUserChallenge.challenge, qChallenge)
                .join(qChallenge.currentRound, qRound)
                .join(qRoundRecord).on(
                        qRoundRecord.round.eq(qRound)
                                .and(qRoundRecord.userChallenge.eq(qUserChallenge))
                )
                .where(
                        qUserChallenge.user.eq(user),
                        qUserChallenge.status.eq(ChallengeJoinStatus.JOINED)
                )
                .orderBy(qChallenge.startDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        // Slice 객체 생성 및 반환
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<UserResponseDto.LikedChallengeDto> findMarkedChallengesByUser(User user, Pageable pageable) {
        QChallengeLike qChallengeLike = QChallengeLike.challengeLike;
        QChallenge qChallenge = QChallenge.challenge;

        // 찜한 챌린지 정보 조회
        List<UserResponseDto.LikedChallengeDto> content = jpaQueryFactory
                .select(Projections.fields(UserResponseDto.LikedChallengeDto.class,
                        qChallenge.id.as("challengeId"),
                        qChallenge.title,
                        qChallenge.description,
                        qChallenge.imageKey.as("image")
                ))
                .from(qChallengeLike)
                .join(qChallengeLike.challenge, qChallenge)
                .where(qChallengeLike.user.eq(user))
                .orderBy(qChallengeLike.createdAt.desc()) // 찜한 순서 (최신순)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        // Slice 객체 생성 및 반환
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<UserResponseDto.CompletedChallengeDto> findCompletedChallengesByUser(User user, Pageable pageable) {
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;
        QChallenge qChallenge = QChallenge.challenge;

        // 종료한 챌린지 정보 조회
        // UserChallenge status는 상관없이, Challenge status가 FINISHED인 것만 조회
        List<UserResponseDto.CompletedChallengeDto> content = jpaQueryFactory
                .select(Projections.fields(UserResponseDto.CompletedChallengeDto.class,
                        qChallenge.id.as("challengeId"),
                        qChallenge.title,
                        qChallenge.description,
                        qChallenge.imageKey.as("image")
                ))
                .from(qUserChallenge)
                .join(qUserChallenge.challenge, qChallenge)
                .where(
                        qUserChallenge.user.eq(user),
                        qChallenge.status.eq(ChallengeStatus.FINISHED),
                        qUserChallenge.status.eq(ChallengeJoinStatus.JOINED)
                )
                .orderBy(qUserChallenge.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        // Slice 객체 생성 및 반환
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
