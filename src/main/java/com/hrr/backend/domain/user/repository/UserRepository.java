package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // ID로 유저 조회 (soft delete 미적용)
    Optional<User> findById(Long id);

    boolean existsByNickname(String nickname);

	// 탈퇴 후 30일이 경과된, 즉 정보 정리 대상 사용자 조회
	@Query("SELECT u.id FROM User u WHERE u.deletedAt <= :deleteThreshold AND u.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.INACTIVE")
	List<Long> findUserIdsToDelete(@Param("deleteThreshold") LocalDateTime deleteThreshold);

    // 팔로워 카운트 벌크 감소
    @Modifying
    @Query("UPDATE User u SET u.followerCount = u.followerCount - 1 " +
            "WHERE u.id IN :userIds AND u.followerCount > 0")
    void decrementFollowerCounts(@Param("userIds") List<Long> userIds);

    // 팔로잉 카운트 벌크 감소
    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount - 1 " +
            "WHERE u.id IN :userIds AND u.followingCount > 0")
    void decrementFollowingCounts(@Param("userIds") List<Long> userIds);

    // 포인트 원자적 증가
    @Modifying
    @Query("UPDATE User u SET u.points = u.points + :amount WHERE u.id = :userId")
    void increasePoints(@Param("userId") Long userId, @Param("amount") long amount);

    // 포인트 원자적 차감 (0 미만으로는 내려가지 않도록 방어)
    @Modifying
    @Query("UPDATE User u SET u.points = CASE WHEN u.points - :amount < 0 THEN 0 ELSE u.points - :amount END WHERE u.id = :userId")
    void decreasePoints(@Param("userId") Long userId, @Param("amount") long amount);
}
