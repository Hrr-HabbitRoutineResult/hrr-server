package com.hrr.backend.domain.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserBlock;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

	// 차단 여부 확인
	boolean existsByBlockerAndBlocked(User blocker, User blocked);

	// 차단 해제
	void deleteByBlockerAndBlocked(User blocker, User blocked);

	/**
	 * 내가 차단한 유저 목록 조회 (User 정보 fetch join) - 탈퇴 상태 사용자 제외
	 * 나를 차단한 사람을 제외하진 않는다.
	 */
	@Query("SELECT b FROM UserBlock b " +
		"JOIN FETCH b.blocked " +
		"WHERE b.blocker = :blocker " +
		"AND b.blocked.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE")
	Slice<UserBlock> findByBlocker(@Param("blocker") User blocker, Pageable pageable);

    /**
     * 상호 차단 관계 확인 (둘 중 한 명이라도 차단했는지 여부)
     */
    default boolean isBlockedRelation(User userA, User userB) {
        if (userA == null || userB == null) return false;
        return existsByBlockerAndBlocked(userA, userB) ||
                existsByBlockerAndBlocked(userB, userA);
    }
  
    // [최적화] 내가 차단한 사람들의 ID만 조회
    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(@Param("blockerId") Long blockerId);

    // [최적화] 나를 차단한 사람들의 ID만 조회
    @Query("SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :blockedId")
    List<Long> findBlockerIdsByBlockedId(@Param("blockedId") Long blockedId);

    @Query("SELECT COUNT(b) > 0 FROM UserBlock b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

}
