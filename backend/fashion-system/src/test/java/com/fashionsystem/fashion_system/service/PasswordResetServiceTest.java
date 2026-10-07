package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.PasswordResetProperties;
import com.fashionsystem.fashion_system.dto.auth.PasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.repository.PasswordResetOtpRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {
    private UserRepository users;
    private PasswordResetOtpRepository otps;
    private PasswordEncoder encoder;
    private MailHelper mail;
    private PasswordResetService service;
    private PasswordResetProperties properties;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        otps = mock(PasswordResetOtpRepository.class);
        encoder = mock(PasswordEncoder.class);
        mail = mock(MailHelper.class);
        properties = new PasswordResetProperties();
        properties.setOtpExpirationMinutes(5);
        properties.setResetTokenExpirationMinutes(10);
        properties.setMaxOtpAttempts(5);
        properties.setResendCooldownSeconds(60);
        service = new PasswordResetService(users, otps, encoder, mail, properties);
    }

    @Test
    void employeeOtpKeepsExistingFiveMinuteContract() {
        User employee = User.builder().id(UUID.randomUUID()).email("employee@lunaria.vn")
                .fullName("Employee").active(true).build();
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        when(otps.findFirstByEmailAndAccountTypeOrderByCreatedAtDesc(
                employee.getEmail(), AccountType.EMPLOYEE)).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("otp-hash");
        LocalDateTime before = LocalDateTime.now();

        service.requestOtp(new PasswordResetOtpRequest(employee.getEmail(), AccountType.EMPLOYEE));

        ArgumentCaptor<PasswordResetOtp> captor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(otps).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isBetween(
                before.plusSeconds(299), LocalDateTime.now().plusSeconds(301));
        verify(mail).sendOtp(eq(employee.getEmail()), eq("Employee"), anyString(), eq(5L));
    }

    @Test
    void internalPasswordResetRejectsCustomerAccountType() {
        assertThatThrownBy(() -> service.requestOtp(
                new PasswordResetOtpRequest("customer@example.com", AccountType.CUSTOMER)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void employeeResetKeepsExistingPasswordAndLockBehavior() throws Exception {
        User employee = User.builder().id(UUID.randomUUID()).passwordHash("old")
                .failedLoginAttempts(3).loginLockedUntil(LocalDateTime.now().plusMinutes(1))
                .locked(false).active(true).build();
        PasswordResetOtp otp = PasswordResetOtp.builder().id(UUID.randomUUID())
                .accountType(AccountType.EMPLOYEE).accountId(employee.getId())
                .email("employee@lunaria.vn").otpHash("otp")
                .resetTokenHash(sha256("reset-token"))
                .verifiedAt(LocalDateTime.now().minusMinutes(1))
                .resetTokenExpiresAt(LocalDateTime.now().plusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(1)).failedAttempts(0)
                .createdAt(LocalDateTime.now()).build();
        when(otps.findUnusedByResetTokenHashForUpdate(otp.getResetTokenHash()))
                .thenReturn(Optional.of(otp));
        when(users.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(encoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest("reset-token", "new-password"));

        assertThat(employee.getPasswordHash()).isEqualTo("new-hash");
        assertThat(employee.getFailedLoginAttempts()).isZero();
        assertThat(employee.getLoginLockedUntil()).isNull();
        verify(users).save(employee);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

