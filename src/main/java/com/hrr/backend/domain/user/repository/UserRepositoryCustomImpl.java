package com.hrr.backend.domain.user.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import com.hrr.backend.domain.user.entity.QUser;
import com.hrr.backend.domain.user.entity.QUserBlock;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	static QUser qUser = QUser.user;
	static QUserBlock qUserBlock = QUserBlock.userBlock;


	/**
	 * 닉네임에 키워드가 포함된 사용자 목록을 Slice 형태로 조회
	 * * @param keyword 검색할 닉네임 키워드
	 *
	 * @param keyword 검색어
	 * @param me 로그인한 사용자
	 * @param pageable 페이징 요청 정보 (page, size)
	 * @return Slice<User> - 데이터 목록과 다음 페이지 존재 여부(hasNext)를 포함
	 */
	@Override
	public Slice<User> findByNicknameContaining(String keyword, User me, Pageable pageable) {
		// 필터링 대상 ID들 수집 (내가 차단한 + 나를 차단한)
		List<Long> blockedUserIds = queryFactory
			.select(qUserBlock.blocked.id)
			.from(qUserBlock)
			.where(qUserBlock.blocker.eq(me))	// 내가 차단한
			.fetch();

		List<Long> blockerUserIds = queryFactory
			.select(qUserBlock.blocker.id)
			.from(qUserBlock)
			.where(qUserBlock.blocked.eq(me))	// 나를 차단한
			.fetch();

		// 두 리스트 합치기
		Set<Long> allRelatedBlockIds = new HashSet<>();
		allRelatedBlockIds.addAll(blockedUserIds);
		allRelatedBlockIds.addAll(blockerUserIds);

		// 실제 검색 쿼리
		List<User> content = queryFactory
			.selectFrom(qUser)
			.where(
				qUser.nickname.contains(keyword),        // 닉네임 검색
				qUser.userStatus.eq(UserStatus.ACTIVE),  // 탈퇴 유저 필터
				qUser.id.notIn(allRelatedBlockIds),      // 차단 관계 유저 필터
				qUser.id.ne(me.getId())                 // 나 자신 제외
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize() + 1)
			.fetch();

		return checkLastPage(pageable, content);
	}

	// Slice 처리를 위한 공통 로직
	private Slice<User> checkLastPage(Pageable pageable, List<User> content) {
		boolean hasNext = content.size() > pageable.getPageSize();
		if (hasNext) {
			content.remove(pageable.getPageSize());
		}
		return new SliceImpl<>(content, pageable, hasNext);
	}
}
