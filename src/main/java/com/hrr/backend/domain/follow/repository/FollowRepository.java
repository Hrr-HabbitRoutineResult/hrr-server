package com.hrr.backend.domain.follow.repository;

import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

   boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

   Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /**
     * 특정 사용자를 팔로우하는 사용자 목록 조회 (팔로워 목록)
     * @param userId 조회할 사용자 ID
     * @return 팔로워 목록
     */
    @Query("SELECT f.follower FROM Follow f WHERE f.following.id = :userId ORDER BY f.createdAt DESC")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 팔로우하는 사용자 목록 조회 (팔로잉 목록)
     * @param userId 조회할 사용자 ID
     * @return 팔로잉 목록
     */
    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
    List<User> findFollowingsByUserId(@Param("userId") Long userId);
}