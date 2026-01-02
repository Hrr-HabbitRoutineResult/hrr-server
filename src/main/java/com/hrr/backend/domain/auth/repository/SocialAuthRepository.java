package com.hrr.backend.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.auth.entity.SocialAuth;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.user.entity.User;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

	// 유저의 소셜 로그인 정보 조회
	Optional<SocialAuth> findByUser(User user);

	// Social ID와 SocialType으로 검색(겹칠 일 거의 없겠지만 혹시 모르니 같이 검색)
	Optional<SocialAuth> findBySocialIdAndSocialType(String socialId, SocialType socialType);

	// 소셜 로그인 정보 삭제
	@Modifying(clearAutomatically = true) // 삭제 후 영속성 컨텍스트를 비워줌
	@Query("DELETE FROM SocialAuth s WHERE s.user = :user")
	void deleteByUser(@Param("user") User user);
}
