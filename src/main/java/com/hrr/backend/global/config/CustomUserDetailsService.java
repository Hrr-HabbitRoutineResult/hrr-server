package com.hrr.backend.global.config;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security에서 사용자 정보를 로드
 * JWT에서 추출한 userId(username)를 기반으로 DB에서 사용자를 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			// username을 Long 타입의 userId로 변환 - JWT에서 userId를 String 형태로 전달 받음
			Long userId = Long.parseLong(username);

			// DB에서 User 엔티티 조회
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND));

			// CustomUserDetails 객체로 변환하여 반환
			return new CustomUserDetails(user);

		}
		// ---- Spring Security 표준 Exception 사용------
		catch (NumberFormatException e) {
			// ID 형식이 숫자가 아닌 경우
			log.warn("[loadUserByUsername] username을 userId로 변환할 수 없습니다. usernameLength={}",
				username != null ? username.length() : 0);
			throw new UsernameNotFoundException("사용자 ID 형식이 올바르지 않습니다.");
		} catch (GlobalException e) {
			log.warn("[loadUserByUsername] User 인증에 실패했습니다. userId={}, errorCode={}",
				username, e.getErrorCode());
			// GlobalException을 UsernameNotFoundException으로 래핑하여 던집니다.
			if (e.getErrorCode() == ErrorCode.AUTH_USER_NOT_FOUND) {
				// 내부 상세 메시지를 숨겨 보안 향상
				throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
			}

			// 그 외 GlobalException 발생
			throw new UsernameNotFoundException("사용자 인증 중 예기치 않은 오류가 발생했습니다.");
		}
	}
}
