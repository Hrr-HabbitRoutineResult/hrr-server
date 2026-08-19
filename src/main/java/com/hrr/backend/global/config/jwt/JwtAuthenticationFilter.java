package com.hrr.backend.global.config.jwt;

import com.hrr.backend.domain.auth.service.JwtService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * JWT 인증 필터
 * 모든 요청의 Authorization 헤더를 검사하게 하고 토큰이 유효하면 SecurityContext에 인증 정보 주입함
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {	// 인증 로직 실행할지 말지 결정
		String path = request.getRequestURI();

		// 모든 공개 경로를 명시적으로 true 반환(인증 불필요) -> 필터 건너뛰고 바로 spring security 권환 확인하러 감
		return path.equals("/") ||
			path.startsWith("/api/v1/auth/login") ||
			path.startsWith("/api/v1/auth/reissue") ||
			path.startsWith("/swagger-ui") ||
			path.startsWith("/v3/api-docs") ||
			path.startsWith("/swagger-resources") ||
			path.startsWith("/webjars");
	}

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = jwtService.resolveToken(request);

		// 블랙리스트 확인
		if (token != null) {
			// 토큰이 블랙리스트에 등록되어 있는지 확인
			if (jwtService.isTokenBlacklisted(token)) {
				// 블랙리스트에 있으면 유효기간이 남아도 즉시 무효화
				SecurityContextHolder.clearContext();

				// 토큰 무효화 관련 GlobalException을 request에 저장하여 예외 핸들러가 처리하도록 함
				request.setAttribute("jwtException", new GlobalException(ErrorCode.AUTH_TOKEN_INVALIDATED));

				// 필터 체인을 계속 진행시켜 다음 필터가 예외를 처리
				chain.doFilter(request, response);
				return;
			}
		}

        // 헤더에 토큰이 있고, SecurityContext에 인증 정보가 없는 경우에만 인증 처리
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 토큰 검증 (유효하지 않으면 GlobalException 던짐)
                jwtService.validateToken(token);

                // userId 추출
                Long userId = jwtService.extractUserId(token);

                // DB에서 유저 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND));

                // CustomUserDetails 객체로 userDetails를 대신함
                UserDetails userDetails = new CustomUserDetails(user);

                //인증 객체 구성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // SecurityContext에 인증 객체 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (GlobalException e) {
                // 토큰 오류 시 SecurityContext를 비우고, 예외를 request에 저장
                SecurityContextHolder.clearContext();
                request.setAttribute("jwtException", e);
            }
        }

        chain.doFilter(request, response);
    }
}
