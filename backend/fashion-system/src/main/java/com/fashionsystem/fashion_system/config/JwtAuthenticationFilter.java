package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.security.AccessTokenAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Đọc Bearer JWT một lần mỗi request và thiết lập SecurityContext. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenAuthenticationService accessTokenAuthenticationService;

    /**
     * Xác minh Bearer token, tải user và đưa authority từ role vào SecurityContext.
     *
     * @param request HTTP request hiện tại
     * @param response HTTP response hiện tại
     * @param filterChain chuỗi filter còn lại
     * @throws ServletException khi filter tiếp theo gặp lỗi servlet
     * @throws IOException khi xử lý request/response gặp lỗi I/O
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            accessTokenAuthenticationService.authenticate(authorization.substring(BEARER_PREFIX.length()))
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }
        filterChain.doFilter(request, response);
    }

}
