package com.hrr.backend.global.config;

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
            //폼 로그인 비활성화(OAuth, JWT로만 처리)
            .formLogin(AbstractHttpConfigurer::disable)

			// 모든 요청 허용(임시)
			// TODO: role 부여 후, 권한별 요청 통제 필요
			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll()
			);

            //실제 서비스용
            /*
            * .authorizeHttpRequests(auth -> auth
                    // Swagger 및 카카오 인증 엔드포인트는 모두 허용
                    .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()
                    // 그 외 요청은 JWT 인증 필요
                    .anyRequest().authenticated()
                );
            * */

		return http.build();
	}

}
