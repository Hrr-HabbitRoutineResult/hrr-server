package com.hrr.backend.global.config;

import com.hrr.backend.global.config.jwt.JwtAuthenticationEntryPoint;
import com.hrr.backend.global.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
			// CSRF 보호 비활성화
			.csrf(AbstractHttpConfigurer::disable)
			// JWT 토큰 기반이므로 세션 사용 안 함
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
            //폼 로그인 비활성화(OAuth, JWT로만 처리)
            .formLogin(AbstractHttpConfigurer::disable)

            //실제 서비스용
             .authorizeHttpRequests(auth -> auth
				 	.requestMatchers(HttpMethod.GET, "/").permitAll()
                    // Swagger 및 카카오 인증 엔드포인트는 모두 허용
                    .requestMatchers(
                        "/api/v1/auth/login/**",	// 로그인 허용
						"/api/v1/auth/reissue/**", // 토큰 재발급 허용
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
					).permitAll()
                    // 그 외 요청은 JWT 인증 필요
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    //.authenticationEntryPoint((req, res, ex1) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                    //.accessDeniedHandler((req, res, ex1) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
            );
            //필터 등록 -> Spring Security 로그인 필터 전에 JwtAuthenticationFilter 실행하도록 등록함
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(List.of(
			"https://*.hrr-umc7.store",  // 운영 환경 (www 포함 모든 서브도메인)
			"http://localhost:*",         // 로컬 개발 환경
			"https://appleid.apple.com",
			"https://semicapitalistic-shanell-irretraceable.ngrok-free.dev"	// 로컬 테스트용 임시
		));
        config.setAllowedOrigins(List.of(
                        "http://localhost:8080",
                        "http://localhost:5173",
                        "http://localhost:3000",
						"https://www.hrr-umc7.store",
						"https://hrr-umc7.store"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
