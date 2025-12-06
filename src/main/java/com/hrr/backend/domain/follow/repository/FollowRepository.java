package com.hrr.backend.domain.follow.repository;

import com.hrr.backend.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

   boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

   Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
}