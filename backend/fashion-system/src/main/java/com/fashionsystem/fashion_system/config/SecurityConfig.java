package com.fashionsystem.fashion_system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình Spring Security stateless với JWT và phân quyền theo role.
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    /**
     * Cấu hình endpoint công khai, session stateless, lỗi 401/403 và các security filter.
     *
     * @param http đối tượng cấu hình bảo mật HTTP
     * @return chuỗi bộ lọc bảo mật đã được cấu hình
     * @throws Exception khi cấu hình bảo mật không thể khởi tạo
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusAccessDeniedHandler(HttpStatus.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/error",
                                "/api/auth/register/customer",
                                "/api/auth/login",
                                "/api/auth/login/employee",
                                "/api/auth/login/customer",
                                "/api/auth/login/customer/social",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/password/**",
                                "/api/products/**",
                                "/ws",
                                "/ws/**",
                                // Webhook hãng vận chuyển: xác thực bằng chữ ký HMAC trong body, không dùng JWT.
                                "/api/webhooks/shipping/**",
                                // IPN / return URL của VNPay, MoMo: xác thực bằng chữ ký của cổng.
                                "/api/webhooks/payments/**",
                                // Health check cho Docker/giám sát; chỉ trả UP/DOWN.
                                "/actuator/health",
                                // Swagger UI; chỉ tồn tại khi API_DOCS_ENABLED=true.
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        // Danh mục sản phẩm cho cửa hàng online: chỉ đọc, khách chưa đăng nhập cũng xem được.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/storefront/**").permitAll()
                        // Xem trước giỏ trên trình duyệt của khách chưa đăng nhập (chỉ đọc, không lưu).
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/storefront/cart/preview").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Tạo BCrypt encoder dùng để băm và so khớp password.
     *
     * @return password encoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS tường minh để cookie credential chỉ đi từ các frontend đã cấu hình. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
