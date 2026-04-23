package com.example.kuriq.security;

import com.example.kuriq.repository.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터.
 *
 * 요청 흐름:
 *  1) Authorization: Bearer {accessToken} 헤더에서 토큰 추출
 *  2) JwtProvider로 서명/만료 검증
 *  3) userId로 DB에서 사용자 조회 (삭제 여부 확인)
 *  4) SecurityContext에 인증 정보 설정
 *
 * 이 필터를 통과한 요청만 @PreAuthorize 또는 .authenticated() 자원에 접근 가능.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            String userId = jwtProvider.getUserId(token);
            // 소프트 삭제된 사용자 차단
            userRepository.findById(userId)
                    .filter(u -> !u.getIsDeleted())
                    .ifPresent(user -> {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
    /** Authorization 헤더에서 "Bearer " 접두사 제거 후 토큰 반환 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
