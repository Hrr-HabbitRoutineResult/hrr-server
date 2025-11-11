package com.hrr.backend.global.config;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
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
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			// username을 Long 타입의 userId로 변환
			Long userId = Long.parseLong(username);

			// DB에서 User 엔티티 조회
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND));

			// 3. PrincipalDetails 객체로 변환하여 반환
			return new CustomUserDetails(user);

		}
		// ---- Spring Security 표준 Exception 사용------
		catch (NumberFormatException e) {
			// ID 형식이 숫자가 아닌 경우
			throw new UsernameNotFoundException("사용자 ID 형식이 올바르지 않습니다: " + username);
		} catch (GlobalException e) {
			// GlobalException을 UsernameNotFoundException으로 래핑하여 던집니다.
			throw new UsernameNotFoundException(e.getMessage());
		}
	}
}
