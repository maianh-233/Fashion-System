package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.AuthProperties;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerSocialAccountRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import com.fashionsystem.fashion_system.validation.AccountRegistrationValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceLoginLockTest {
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthAuditService authAuditService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authAuditService = mock(AuthAuditService.class);
        AuthProperties properties = new AuthProperties();
        properties.setMaxFailedAttempts(5);
        properties.setLoginLockDurationSeconds(60);
        service = new AuthService(
                userRepository,
                mock(CustomerRepository.class),
                mock(CustomerSocialAccountRepository.class),
                mock(UserDepartmentRepository.class),
                mock(RevokedTokenRepository.class),
                roleRepository,
                mock(UserRoleRepository.class),
                passwordEncoder,
                jwtService,
                List.of(),
                mock(AccountRegistrationValidator.class),
                new AuthResponseMapper(),
                properties,
                authAuditService);
    }

    @Test
    void fifthWrongPasswordCreatesDatabaseBackedOneMinuteLock() {
        User user = activeUser();
        user.setFailedLoginAttempts(4);
        when(userRepository.findByUsernameForUpdate("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);
        LocalDateTime before = LocalDateTime.now();

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getHeaders().getFirst("Retry-After")).isEqualTo("60");
                });

        assertThat(user.getLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLoginLockedUntil()).isAfterOrEqualTo(before.plusSeconds(59));
        assertThat(user.getLoginLockedUntil()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(61));
        verify(userRepository).save(user);
    }

    @Test
    void temporaryLockRejectsCorrectPasswordUntilDatabaseDeadline() {
        User user = activeUser();
        user.setLoginLockedUntil(LocalDateTime.now().plusSeconds(45));
        when(userRepository.findByUsernameForUpdate("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "correct")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(passwordEncoder, never()).matches("correct", user.getPasswordHash());
    }

    @Test
    void expiredTemporaryLockIsClearedAndCorrectPasswordCanLogin() {
        User user = activeUser();
        user.setFailedLoginAttempts(3);
        user.setLoginLockedUntil(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByUsernameForUpdate("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", user.getPasswordHash())).thenReturn(true);
        when(roleRepository.findCodesByUserId(user.getId())).thenReturn(List.of("ADMIN"));
        when(jwtService.generateToken(user, List.of("ADMIN"))).thenReturn("token");
        when(jwtService.getExpirationMs()).thenReturn(1800000L);

        service.login(new LoginRequest("admin", "correct"));

        assertThat(user.getLoginLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(user);
    }

    private User activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@lunaria.vn")
                .passwordHash("hash")
                .active(true)
                .locked(false)
                .failedLoginAttempts(0)
                .build();
    }
}
