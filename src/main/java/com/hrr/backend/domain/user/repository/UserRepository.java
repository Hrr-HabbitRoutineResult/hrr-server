package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // ID로 유저 조회 (soft delete 미적용)
    Optional<User> findById(Long id);

    boolean existsByNickname(String nickname);

	// 탈퇴 후 30일이 경과된, 즉 정보 정리 대상 사용자 조회
	@Query("SELECT u FROM User u WHERE u.deletedAt <= :deleteThreshold AND u.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.DELETED")
	List<User> findUserToDelete(LocalDateTime deleteThreshold);
}
