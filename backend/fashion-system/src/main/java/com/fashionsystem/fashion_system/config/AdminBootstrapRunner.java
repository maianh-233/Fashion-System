package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.entity.EmploymentStatus;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo tài khoản SUPER_ADMIN đầu tiên cho database mới dựng bằng Flyway, vì
 * {@code /api/auth/register/admin} cũng đòi quyền admin nên không có cách nào
 * khác để đăng nhập lần đầu. Chỉ chạy khi đã đặt
 * {@code BOOTSTRAP_ADMIN_USERNAME}/{@code BOOTSTRAP_ADMIN_PASSWORD} và chưa có
 * SUPER_ADMIN nào, nên để nguyên biến môi trường cũng không tạo lại tài khoản.
 */
@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.admin.username:}") String username,
            @Value("${bootstrap.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim().toLowerCase();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isEmpty() || password.isEmpty()) return;

        Role role = roleRepository.findByCode(SUPER_ADMIN).orElse(null);
        if (role == null) {
            log.warn("Admin bootstrap skipped: role {} is missing (reference data not loaded?)", SUPER_ADMIN);
            return;
        }
        if (userRoleRepository.existsByRoleId(role.getId())) return;
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Admin bootstrap skipped: username '{}' already exists without {}", username, SUPER_ADMIN);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(User.builder()
                .username(username)
                .fullName("Super Admin")
                .passwordHash(passwordEncoder.encode(password))
                .active(true)
                .locked(false)
                .employmentStatus(EmploymentStatus.ACTIVE.name())
                .failedLoginAttempts(0)
                .emailVerified(false)
                .phoneVerified(false)
                .lastPasswordChange(now)
                .createdAt(now)
                .build());
        userRoleRepository.save(UserRole.builder()
                .userId(user.getId())
                .roleId(role.getId())
                .assignedAt(now)
                .build());
        log.info("Created bootstrap {} account '{}'", SUPER_ADMIN, username);
    }
}
