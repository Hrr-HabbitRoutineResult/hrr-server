package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final QUser qUser = QUser.user;

    @Override
    public Optional<UserResponseDto> findUserProfileById(Long myId, Long targetUserId) {

        UserResponseDto dto = jpaQueryFactory
                .select(Projections.fields(UserResponseDto.class,
                        qUser.id.as("userId"),
                        qUser.nickname,
                        qUser.profilePhoto,
                        qUser.level.stringValue().as("level"),
                        qUser.followerCount,
                        qUser.followingCount,

                        //[임시 조치] isFollowing 필드를 'false' 상수로 고정
                        Expressions.as(Expressions.constant(false), "isFollowing")

                ))
                .from(qUser)
                .where(qUser.id.eq(targetUserId))
                .fetchOne();

        return Optional.ofNullable(dto);
    }
}