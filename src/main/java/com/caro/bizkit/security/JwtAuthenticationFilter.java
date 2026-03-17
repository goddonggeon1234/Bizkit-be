package com.caro.bizkit.security;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserPrincipalCacheService userPrincipalCacheService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserPrincipalCacheService userPrincipalCacheService,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPrincipalCacheService = userPrincipalCacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return  path.startsWith("/api/auth/login") ||
                path.equals("/api/auth/rotation") ||
                path.equals("/api/auth/kakao/callback") ||
                path.startsWith("/api/cards/uuid/") ||
                path.startsWith("/ws") ||
                path.equals("/error") ||
                path.equals("/chat-test.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException
    {
        String token = extractTokenFromHeader(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!jwtTokenProvider.isValid(token)) {
            log.warn("Invalid token: method={}, path={}", request.getMethod(), request.getRequestURI());
            writeUnauthorizedResponse(response, "유효하지 않은 토큰입니다. 서명 또는 만료 시간을 확인하세요.");
            return;
        }

        Claims claims = jwtTokenProvider.parseClaims(token);
        Integer userId = parseUserId(claims.getSubject());
        if (userId == null) {
            log.warn("Invalid token subject: method={}, path={}", request.getMethod(), request.getRequestURI());
            writeUnauthorizedResponse(response, "유효하지 않은 토큰입니다. 사용자 식별자가 올바르지 않습니다.");
            return;
        }
        UserPrincipal principal = userPrincipalCacheService.findById(userId);

        if (principal == null) {
            log.warn("User not found for token: method={}, path={}, userId={}",
                    request.getMethod(), request.getRequestURI(), userId);
            writeUnauthorizedResponse(response, "유효하지 않은 토큰입니다. 사용자 정보를 찾을 수 없습니다.");
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private Integer parseUserId(String subject) {
        try {
            return Integer.valueOf(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        ApiResponse<Void> body = ApiResponse.failed(HttpStatus.UNAUTHORIZED, message);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
