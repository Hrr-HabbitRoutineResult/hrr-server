package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // ID로 유저 조회 (soft delete 미적용)
    Optional<User> findById(Long id);
    // Kakao ID
    Optional<User> findByKakaoId(Long kakaoId);
}
