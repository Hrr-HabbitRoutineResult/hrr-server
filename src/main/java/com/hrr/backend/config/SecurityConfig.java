package com.hrr.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// CSRF 보호 비활성화
			.csrf(AbstractHttpConfigurer::disable)

			// JWT 토큰 기반이므로 세션 사용 안 함
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)

			// 모든 요청 허용(임시)
			// TODO: role 부여 후, 권한별 요청 통제 필요
			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll()
			);

		return http.build();
	}

}
