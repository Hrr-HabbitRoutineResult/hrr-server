package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.challenge.entity.QChallengeLike;
import com.hrr.backend.domain.round.entity.QRound;
import com.hrr.backend.domain.round.entity.QRoundRecord;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.QUser;
import com.hrr.backend.domain.user.entity.QUserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
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
                .leftJoin(qChallenge.currentRound, qRound)
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
    public Slice<UserResponseDto.LikedChallengeDto> findMarkedChallengesByUser(User user, List<Long> blockedUserIds, Pageable pageable) {
        QChallengeLike qChallengeLike = QChallengeLike.challengeLike;
        QChallenge qChallenge = QChallenge.challenge;
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;

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
                .join(qUserChallenge).on(qUserChallenge.challenge.eq(qChallenge)
                        .and(qUserChallenge.role.eq(UserChallengeRole.OWNER)))
                .where(
                        qChallengeLike.user.eq(user),
                        // 차단한 유저 ID 목록이 비어있지 않을 때만 notIn 조건 적용
                        blockedUserIds.isEmpty() ? null : qUserChallenge.user.id.notIn(blockedUserIds)
                )
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
        // UserChallenge status는 완주한(JOINED) 챌린지만 그룹화하여 조회
        // 한 챌린지에 여러 라운드 참여해도 챌린지는 1개만 조회
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
                        qUserChallenge.status.eq(ChallengeJoinStatus.DROPPED)
                )
                .groupBy(qChallenge.id)
                .orderBy(qUserChallenge.updatedAt.max().desc())
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
    // 챌린지 참가 중인 챌린저 목록 조회
    @Override
    public Slice<UserChallenge> findParticipantsByChallengeId(
            Long challengeId,
            Long currentUserId,
            List<Long> excludedUserIds,
            Pageable pageable
    ) {
        QUserChallenge qUserChallenge = QUserChallenge.userChallenge;
        QUser qUser = QUser.user;

        // 참가 중인 챌린저 조회
        // - UserChallenge가 JOINED 상태인 유저만 조회
        // - 탈퇴/정지 등 비활성 유저는 제외 (ACTIVE만)
        // - 차단 관계인 유저는 제외
        // - 정렬: 본인 1순위 -> 방장 2순위 -> 닉네임 오름차순
        List<UserChallenge> content = jpaQueryFactory
                .selectFrom(qUserChallenge)
                .join(qUserChallenge.user, qUser).fetchJoin()
                .where(
                        qUserChallenge.challenge.id.eq(challengeId),
                        qUserChallenge.status.eq(ChallengeJoinStatus.JOINED),
                        qUser.userStatus.eq(UserStatus.ACTIVE),
                        notInExcludedUsers(qUser, excludedUserIds)
                )
                .orderBy(
                        new CaseBuilder()
                                .when(qUser.id.eq(currentUserId)).then(0)	// 본인 1순위
                                .when(qUserChallenge.role.eq(UserChallengeRole.OWNER)).then(1)	// 방장 2순위
                                .otherwise(2)
                                .asc(),
                        qUser.nickname.asc()
                )
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

    /**차단 관계 유저 제외 조건 */
    private BooleanExpression notInExcludedUsers(QUser qUser, List<Long> excludedUserIds) {
        if (excludedUserIds == null || excludedUserIds.isEmpty()) {
            return null;
        }
        return qUser.id.notIn(excludedUserIds);
    }
}
