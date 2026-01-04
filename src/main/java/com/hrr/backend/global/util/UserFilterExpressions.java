package com.hrr.backend.global.util;

import com.hrr.backend.domain.user.entity.QUser;
import com.hrr.backend.domain.user.entity.QUserBlock;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;

public class UserFilterExpressions {
	private static final QUser qUser = QUser.user;
	private static final QUserBlock qUserBlock = QUserBlock.userBlock;

	// 활성 유저 조건 (탈퇴/비활성화 제외)
	public static BooleanExpression isStatusActive() {
		return qUser.userStatus.eq(UserStatus.ACTIVE);
	}

	// 내가 차단한 유저 제외
	public static BooleanExpression notBlockedByMe(Long myId) {
		if (myId == null) return null;
		return qUser.id.notIn(
			JPAExpressions
				.select(qUserBlock.blocked.id)
				.from(qUserBlock)
				.where(qUserBlock.blocker.id.eq(myId))
		);
	}

	// 나를 차단한 유저 제외
	public static BooleanExpression notBlockingMe(Long meId) {
		if (meId == null) return null;
		return qUser.id.notIn(
			JPAExpressions
				.select(qUserBlock.blocker.id)
				.from(qUserBlock)
				.where(qUserBlock.blocked.id.eq(meId))
		);
	}
}
