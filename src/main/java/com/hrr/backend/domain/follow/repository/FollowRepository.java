package com.hrr.backend.domain.follow.repository;

import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 팔로우 관계 존재 여부 확인 (상태 무관)
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 팔로우 관계 조회 (상태 무관)
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /**
     * 특정 상태의 팔로우 관계 조회
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @param status 팔로우 상태
     * @return 팔로우 관계
     */
    Optional<Follow> findByFollowerIdAndFollowingIdAndStatus(Long followerId, Long followingId, FollowStatus status);

    /**
     * 특정 사용자를 팔로우하는 사용자 목록 조회 (팔로워 목록) - APPROVED만 + ACTIVE 유저만
     * @param userId 조회할 사용자 ID
     * @param pageable 페이징 정보
     * @return 팔로워 목록 (Slice)
     */
    @Query("SELECT f.follower FROM Follow f " +
            "WHERE f.following.id = :userId " +
            "AND f.status = 'APPROVED' " +
            "AND f.follower.userStatus = 'ACTIVE' " +
            "AND f.follower.id NOT IN ( SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :currentUserId )" +
            "AND f.follower.id NOT IN ( SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :currentUserId )" +
            "ORDER BY f.createdAt DESC")
    Slice<User> findFollowersByUserId(
            @Param("userId") Long userId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    /**
     * 특정 사용자가 팔로우하는 사용자 목록 조회 (팔로잉 목록) - APPROVED만 + ACTIVE 유저만
     * @param userId 조회할 사용자 ID
     * @param pageable 페이징 정보
     * @return 팔로잉 목록 (Slice)
     */
    @Query("SELECT f.following FROM Follow f " +
            "WHERE f.follower.id = :userId " +
            "AND f.status = 'APPROVED' " +
            "AND f.following.userStatus = 'ACTIVE' " +
            "AND f.following.id NOT IN (SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :currentUserId) " +
            "AND f.following.id NOT IN (SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :currentUserId) " +
            "ORDER BY f.createdAt DESC")
    Slice<User> findFollowingsByUserId(
            @Param("userId") Long userId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    /**
     * 특정 사용자가 팔로우 중인 사용자 ID 목록 조회 (N+1 문제 해결용) - APPROVED만
     * @param followerId 팔로워 ID
     * @param followingIds 확인할 팔로잉 ID 목록
     * @return 팔로우 중인 사용자 ID 목록
     */
    @Query("SELECT f.following.id FROM Follow f " +
            "WHERE f.follower.id = :followerId " +
            "AND f.following.id IN :followingIds " +
            "AND f.status = 'APPROVED'")
    List<Long> findFollowingIdsByFollowerIdAndFollowingIds(@Param("followerId") Long followerId, @Param("followingIds") List<Long> followingIds);

    /**
     * 받은 팔로우 요청 목록 조회 (PENDING 상태)
     * @param userId 사용자 ID (요청 받은 사람)
     * @param status 팔로우 상태
     * @return 팔로우 요청 목록
     */
    @Query("SELECT f FROM Follow f " +
            "WHERE f.following.id = :userId " +
            "AND f.status = :status " +
            "AND f.follower.userStatus = 'ACTIVE' " + // 탈퇴 유저 제외
            "AND f.follower.id NOT IN ( " +           // 나를 차단한 유저 제외
            "   SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :userId" +
            ") " +
            "ORDER BY f.createdAt DESC")
    Slice<Follow> findPendingFollowRequests(@Param("userId") Long userId, @Param("status") FollowStatus status, Pageable pageable);

    /**
     * 내가 팔로우하는 모든 관계 조회 (탈퇴 처리 시 사용)
     */
    List<Follow> findAllByFollowerIdAndStatus(Long followerId, FollowStatus status);

    /**
     * 나를 팔로우하는 모든 관계 조회 (탈퇴 처리 시 사용)
     */
    List<Follow> findAllByFollowingIdAndStatus(Long followingId, FollowStatus status);

    /**
     * 나를 팔로우하는 '활동 중인' 사용자 수 조회
     * @param userId 사용자 ID
     * @return 활동 중인 팔로워 수
     */
    @Query("SELECT COUNT(f) FROM Follow f " +
            "WHERE f.following.id = :userId " +
            "AND f.status = 'APPROVED' " +
            "AND f.follower.userStatus = 'ACTIVE'")
    long countActiveFollowers(@Param("userId") Long userId);

    /**
     * 내가 팔로우하는 '활동 중인' 사용자 수 조회
     * @param userId 사용자 ID
     * @return 활동 중인 팔로잉 수
     */
    @Query("SELECT COUNT(f) FROM Follow f " +
            "WHERE f.follower.id = :userId " +
            "AND f.status = 'APPROVED' " +
            "AND f.following.userStatus = 'ACTIVE'")
    long countActiveFollowings(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Follow f " +
            "WHERE f.follower.id = :userId " +
            "OR f.following.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
