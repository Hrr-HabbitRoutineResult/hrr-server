package com.hrr.backend.domain.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

	// 차단 여부 확인
	boolean existsByBlockerAndBlocked(User blocker, User blocked);

	// 차단 해제
	void deleteByBlockerAndBlocked(User blocker, User blocked);

	/**
	 * 내가 차단한 유저 목록 조회 (User 정보 fetch join) - 탈퇴 상태 사용자 제외
	 */
	@Query("SELECT b FROM UserBlock b " +
		"JOIN FETCH b.blocked " +
		"WHERE b.blocker = :blocker " +
		"AND b.blocked.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE")
	Slice<UserBlock> findByBlocker(@Param("blocker") User blocker, Pageable pageable);
}
