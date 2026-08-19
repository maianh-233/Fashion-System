package com.fashionsystem.fashion_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật tạm thời để kiểm thử Product API bằng Postman.
 */
@Configuration
public class SecurityConfig {

    /**
     * Cho phép truy cập Product API không cần đăng nhập và tắt CSRF cho REST API.
     *
     * @param http đối tượng cấu hình bảo mật HTTP
     * @return chuỗi bộ lọc bảo mật đã được cấu hình
     * @throws Exception khi cấu hình bảo mật không thể khởi tạo
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/products/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
