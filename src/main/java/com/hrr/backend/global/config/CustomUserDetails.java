package com.hrr.backend.global.config;

import com.hrr.backend.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security Context에 저장되는 사용자 인증 정보 객체
 * User 엔티티를 래핑하여 DB 재조회 없이 사용자 정보에 접근할 수 있게 해줌
 */
@Getter
public class CustomUserDetails implements UserDetails {
	private final User user; // DB에서 조회된 실제 사용자 엔티티

	public CustomUserDetails(User user) {
		this.user = user;
	}

	// --- UserDetails 필수 메서드 override ---

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// 모든 인증된 사용자에게 'ROLE_USER' 권한을 부여(임시)
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

/*		// User에서 권한을 가져와 부여
		return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));*/
	}

	@Override
	public String getPassword() {
		// JWT 기반이므로 비밀번호는 미사용
		return null;
	}

	@Override
	public String getUsername() {
		// 여기서의 name=사용자를 식별하는 고유값이기 떄문에 user_id를 사용
		return String.valueOf(user.getId());
	}

	// JWT 기반에서는 토큰의 유효성으로 검증을 관리하므로 모두 true를 반환
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
