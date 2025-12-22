package com.hrr.backend.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.auth.entity.SocialAuth;
import com.hrr.backend.domain.auth.entity.enums.SocialType;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

	// Social ID와 SocialType으로 검색(겹칠 일 거의 없겠지만 혹시 모르니 같이 검색)
	Optional<SocialAuth> findBySocialIdAndSocialType(String socialId, SocialType socialType);
}
