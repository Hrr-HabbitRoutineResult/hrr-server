package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.dto.NaverUserResponse;
import com.hrr.backend.domain.auth.entity.SocialAuth;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.auth.repository.SocialAuthRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SocialUserService {
    private final UserRepository userRepository;
	private final SocialAuthRepository socialAuthRepository;

    // 카카오 응답 → 우리 User db에 upsert(있으면 update하고 없으면 insert)
    @Transactional
    public User upsertKakaoUser(KakaoUserResponse kakao) {
        String socialId = String.valueOf(kakao.getId());
        String name = Optional.ofNullable(kakao.getKakaoAccount())
                .map(KakaoUserResponse.KakaoAccount::getProfile)
                .map(KakaoUserResponse.KakaoAccount.Profile::getNickname)
                .orElse("카카오유저");

        String profileImage = Optional.ofNullable(kakao.getKakaoAccount())
                .map(KakaoUserResponse.KakaoAccount::getProfile)
                .map(KakaoUserResponse.KakaoAccount.Profile::getProfile_image_url)
                .orElse(null);

		return socialAuthRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO)
			.map(socialAuth -> {
				// [기존 유저] 연관된 User 정보를 업데이트
				User user = socialAuth.getUser();

				isDeleted(user);	// 탈퇴 여부를 확인

				user.updateName(name);
				user.updateProfileImage(profileImage);
				return user;
			})
			.orElseGet(() -> {
				// [신규 유저] User 생성 후 SocialAuth와 연결하여 저장

				// User 생성
				User newUser = User.signUp(name, profileImage);
				userRepository.save(newUser);

				// SocialAuth 생성 및 연결
				SocialAuth auth = SocialAuth.builder()
					.user(newUser)
					.socialId(socialId)
					.socialType(SocialType.KAKAO)
					.build();
				socialAuthRepository.save(auth);

				return newUser;
			});
    }

	@Transactional
	public User upsertAppleUser(String appleSub, String appleRefreshToken, String name) {
		// socialId(sub)로 기존 유저 확인
		return socialAuthRepository.findBySocialIdAndSocialType(appleSub, SocialType.APPLE)
			.map(socialAuth -> {
				// [기존 유저] 연관된 User 정보를 가져옴
				User user = socialAuth.getUser();

				isDeleted(user);	// 탈퇴 여부를 확인

				// 애플은 이름이 첫 로그인 이후 null 로 올 수 있으므로 , 값이 있을 때만 업데이트
				if (StringUtils.hasText(name)) {
					user.updateName(name);
				}

				// 애플 RT 업데이트 (나중에 탈퇴 시 사용)
				if (StringUtils.hasText(appleRefreshToken)) {
					socialAuth.updateRefreshToken(appleRefreshToken);
				}

				return user;
			})
			.orElseGet(() -> {
				// [신규 유저] User 생성 후 SocialAuth 와 연결하여 저장

				// User 생성 (애플은 프로필 이미지가 없으므로 null 전달)
				String userName = (name != null) ? name : "애플 유저";	// 혹시 모를 이름 누락 대비
				User newUser = User.signUp(userName, null);

				userRepository.save(newUser);

				// SocialAuth 생성 및 애플 전용 RT 저장
				SocialAuth auth = SocialAuth.builder()
					.user(newUser)
					.socialId(appleSub)
					.socialType(SocialType.APPLE)
					.socialRefreshToken(appleRefreshToken)
					.build();
				socialAuthRepository.save(auth);

				return newUser;
			});
	}

	@Transactional
	public User upsertNaverUser(NaverUserResponse naverUserResponse, String refreshToken) {
		// 네이버 응답에서 실제 유저 정보 추출
		NaverUserResponse.Response response = naverUserResponse.getResponse();

		if (response == null) {
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		}

		String naverSocialId = response.getId();
		String email = response.getEmail();
		String name = response.getName();
		String profileImage = response.getProfileImage();

		// 기존 유저 확인
		return socialAuthRepository.findBySocialIdAndSocialType(naverSocialId, SocialType.NAVER)
			.map(socialAuth -> {
				// [기존 유저] 연관된 User 정보를 가져옴
				User user = socialAuth.getUser();

				isDeleted(user);	// 탈퇴 여부를 확인

				// 유저 정보 업데이트
				if (name != null) {
					user.updateName(name);
				}
				// 프로필 이미지 업데이트
				if (profileImage != null) {
					user.updateProfileImage(profileImage);
				}
				// 이메일 업데이트
				if (email != null) {
					user.updateEmail(email);
				}

				// Todo: 네이버 RT 업데이트 (나중에 탈퇴 시 사용)

				return user;
			})
			.orElseGet(() -> {
				// [신규 유저] User 생성 후 SocialAuth 와 연결하여 저장

				// User 생성
				String userName = (name != null) ? name : "네이버 유저";
				User newUser = User.signUp(userName, profileImage);

				if (email != null) {
					newUser.updateEmail(email);
				}

				userRepository.save(newUser);

				// SocialAuth 생성
				SocialAuth auth = SocialAuth.builder()
					.user(newUser)
					.socialId(naverSocialId)
					.socialType(SocialType.NAVER)
					.socialRefreshToken(refreshToken)
					.build();
				socialAuthRepository.save(auth);

				return newUser;
			});
	}

	/**
	 * DELETED 상태, 즉 탈퇴 후 한 달 이내인 계정인지 판단
	 * @param user
	 */
	private void isDeleted(User user) {
		if (user.getUserStatus() == UserStatus.DELETED) {
			throw new GlobalException(ErrorCode.AUTH_WITHDRAWAL_PERIOD_RESTRICTION);
		}
	}
}
