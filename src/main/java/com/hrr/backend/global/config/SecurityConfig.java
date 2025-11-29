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
                        "/api/v1/auth/**",
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
            );;
            //필터 등록 -> Spring Security 로그인 필터 전에 JwtAuthenticationFilter 실행하도록 등록함
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

/*	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new ForwardedHeaderFilter());
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 먼저 실행되도록 순위 조작
		return bean;
	}*/

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(List.of(
			"https://*.hrr-umc7.store",  // 운영 환경 (www 포함 모든 서브도메인)
			"http://localhost:*"));         // 로컬 개발 환경
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

/*package com.hrr.backend.global.config;

import com.hrr.backend.global.config.jwt.JwtAuthenticationEntryPoint;
import com.hrr.backend.global.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

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
			// [수정] corsConfigurationSource() 메서드를 올바르게 호출
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/").permitAll()
				.requestMatchers(
					"/api/v1/auth/**",
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/swagger-resources/**",
					"/webjars/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
			);

		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	*//**
	 * 1. CORS 설정의 핵심 (규칙 정의)
	 * - 여기서 정의한 규칙을 Security와 Filter 양쪽에서 갖다 씁니다.
	 *//*
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(List.of("*")); // 모든 출처 허용
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
		config.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	*//**
	 * 2. CORS 필터 강제 등록 (핵폭탄급 우선순위)
	 * - 위에서 만든 corsConfigurationSource를 사용하여
	 * - Spring Security 필터보다도 *먼저* 실행되게 만듭니다.
	 *//*
	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		// 위에서 만든 corsConfigurationSource()를 재사용합니다.
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
			new CorsFilter(corsConfigurationSource())
		);
		// 가장 높은 우선순위 부여 (지구 끝까지 쫓아가서 CORS 허용함)
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}

	*//**
	 * 3. HTTPS 프로토콜 감지 필터
	 * - Nginx가 보내준 "나 HTTPS야!" 신호를 Spring이 알아먹게 함
	 *//*
	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new ForwardedHeaderFilter());
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}
}*/
