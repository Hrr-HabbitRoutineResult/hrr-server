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
import java.util.Optional;

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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        //인증 불필요 구간은 필터 완전 우회
        if (uri.startsWith("/api/v1/auth")) {
            chain.doFilter(request, response);
            return;
        }
        String token = jwtService.resolveToken(request);

        // 헤더에 토큰이 없으면 그냥 다음 필터로 진행
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 토큰 검증 (유효하지 않으면 GlobalException 던짐)
            jwtService.validateToken(token);

            // userId 추출
            Long userId = jwtService.extractUserId(token);

            // DB에서 유저 조회
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                throw new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND);
            }

            User user = optionalUser.get();

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
            // 토큰 오류 시 필터 체인만 통과 (예외 전파 X)
            request.setAttribute("jwtException", e);
        }

        chain.doFilter(request, response);
    }
}
