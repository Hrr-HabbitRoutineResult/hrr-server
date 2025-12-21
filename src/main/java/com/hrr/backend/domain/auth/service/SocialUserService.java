package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SocialUserService {
    private final UserRepository userRepository;

    public SocialUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 카카오 응답 → 우리 User db에 upsert(있으면 update하고 없으면 insert)
    @Transactional
    public User upsertKakaoUser(KakaoUserResponse kakao) {
        Long kakaoId = kakao.getId();
        String name = Optional.ofNullable(kakao.getKakaoAccount())
                .map(KakaoUserResponse.KakaoAccount::getProfile)
                .map(KakaoUserResponse.KakaoAccount.Profile::getNickname)
                .orElse("카카오유저");

        String profileImage = Optional.ofNullable(kakao.getKakaoAccount())
                .map(KakaoUserResponse.KakaoAccount::getProfile)
                .map(KakaoUserResponse.KakaoAccount.Profile::getProfile_image_url)
                .orElse(null);

        return userRepository.findBySocialId(String.valueOf(kakaoId))
                .map(user -> {
                    user.updateName(name);
                    user.updateProfileImage(profileImage);
                    return user;
                })
                .orElseGet(() -> {
                    // 신규 유저는 NEW 상태로 생성되게 변경
                    return userRepository.save(User.newKakao(kakaoId, name, profileImage));
                });
    }

	@Transactional
	public User upsertAppleUser(String appleSub, String appleRefreshToken, String name) {
		// socialId(sub)로 기존 유저 확인
		return userRepository.findBySocialId(appleSub)
			.map(user -> {
				// 기존 유저라면 애플 RT만 업데이트 (나중에 탈퇴 시 사용)
				// Todo: RT 업데이트
				return user;
			})
			.orElseGet(() -> {
				// 신규 유저라면 회원가입 처리
				User newUser = User.builder()
					.socialId(appleSub)
					.name(name)
					.loginStatus(LoginStatus.NEW)
					.build();
				return userRepository.save(newUser);
			});
	}
}
