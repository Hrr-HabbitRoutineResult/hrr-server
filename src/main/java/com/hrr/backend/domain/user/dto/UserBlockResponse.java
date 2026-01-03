package com.hrr.backend.domain.user.dto;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserLevel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UserBlockResponse {
	private final Long userId;

	private final String nickname;

	private final UserLevel level;

	private final boolean isBlocked; // 차단 여부

	public UserBlockResponse(User user) {
		this.userId = user.getId();
		this.nickname = user.getDisplayNickname();
		this.level = user.getUserLevel();
		this.isBlocked = true; // 차단 목록에 있으므로 항상 true
	}
}
