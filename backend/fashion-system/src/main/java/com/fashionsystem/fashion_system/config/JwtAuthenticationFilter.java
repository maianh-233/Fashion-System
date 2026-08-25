package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.JwtService;
import com.fashionsystem.fashion_system.service.RbacService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Đọc Bearer JWT một lần mỗi request và thiết lập SecurityContext. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;
    private final RevokedTokenRepository revokedTokenRepository;

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
            authenticate(authorization.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra token rồi nạp role/permission hiện hành từ DB để tạo Authentication.
     *
     * @param token JWT lấy từ Authorization header
     */
    private void authenticate(String token) {
        try {
            if (revokedTokenRepository.existsById(jwtService.extractTokenId(token))) {
                return;
            }
            if (JwtService.ACCOUNT_TYPE_CUSTOMER.equals(jwtService.extractAccountType(token))) {
                authenticateCustomer(token);
                return;
            }
            UUID userId = jwtService.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())
                    || user.getDeletedAt() != null || !jwtService.isTokenValid(token, user)) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(user.getId(), user.getUsername()),
                            null,
                            rbacService.loadAuthorities(user.getId()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateCustomer(String token) {
        UUID customerId = jwtService.extractCustomerId(token);
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null || !Boolean.TRUE.equals(customer.getActive())
                || Boolean.TRUE.equals(customer.getLocked())
                || !jwtService.isCustomerTokenValid(token, customer)) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedCustomer(customer.getId(), customer.getUsername()),
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
