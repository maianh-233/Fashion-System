package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.PasswordResetProperties;
import com.fashionsystem.fashion_system.dto.auth.CustomerPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerPasswordResetChallenge;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerPasswordResetChallengeRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
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

class CustomerPasswordResetServiceTest {
    private CustomerAccountRepository accounts;
    private CustomerRepository customers;
    private CustomerPasswordResetChallengeRepository challenges;
    private PasswordEncoder encoder;
    private MailHelper mail;
    private CustomerRefreshTokenService refreshTokens;
    private CustomerPasswordResetService service;
    private PasswordResetProperties properties;

    @BeforeEach
    void setUp() {
        accounts = mock(CustomerAccountRepository.class);
        customers = mock(CustomerRepository.class);
        challenges = mock(CustomerPasswordResetChallengeRepository.class);
        encoder = mock(PasswordEncoder.class);
        mail = mock(MailHelper.class);
        refreshTokens = mock(CustomerRefreshTokenService.class);
        properties = new PasswordResetProperties();
        properties.setCustomerOtpExpirationMinutes(1);
        properties.setResetTokenExpirationMinutes(10);
        properties.setMaxOtpAttempts(5);
        properties.setResendCooldownSeconds(60);
        service = new CustomerPasswordResetService(
                accounts, customers, challenges, encoder, mail, refreshTokens, properties);
    }

    @Test
    void requestCreatesCustomerOnlyChallengeWithOneMinuteExpiry() {
        Customer customer = Customer.builder().id(UUID.randomUUID()).fullName("Customer").active(true).build();
        CustomerAccount account = account(customer.getId());
        when(accounts.findByLoginEmailIgnoreCase(account.getLoginEmail())).thenReturn(Optional.of(account));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(challenges.findFirstByEmailOrderByCreatedAtDesc(account.getLoginEmail()))
                .thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("otp-hash");
        LocalDateTime before = LocalDateTime.now();

        service.requestOtp(new CustomerPasswordResetOtpRequest(account.getLoginEmail()));

        ArgumentCaptor<CustomerPasswordResetChallenge> captor =
                ArgumentCaptor.forClass(CustomerPasswordResetChallenge.class);
        verify(challenges).save(captor.capture());
        assertThat(captor.getValue().getCustomerAccountId()).isEqualTo(account.getId());
        assertThat(captor.getValue().getExpiresAt()).isBetween(
                before.plusSeconds(59), LocalDateTime.now().plusSeconds(61));
        verify(mail).sendOtp(eq(account.getLoginEmail()), eq("Customer"), anyString(), eq(1L));
    }

    @Test
    void unknownEmailKeepsGenericResponseWithoutPersistence() {
        var response = service.requestOtp(new CustomerPasswordResetOtpRequest("unknown@example.com"));

        assertThat(response.message()).doesNotContain("unknown@example.com");
        verify(challenges, never()).save(any());
        verify(mail, never()).sendOtp(anyString(), anyString(), anyString(), any(Long.class));
    }

    @Test
    void resetUpdatesCustomerAccountClearsLockAndRevokesCustomerSessions() throws Exception {
        CustomerAccount account = account(UUID.randomUUID());
        account.setFailedLoginAttempts(3);
        account.setLoginLockedUntil(LocalDateTime.now().plusMinutes(1));
        CustomerPasswordResetChallenge challenge = CustomerPasswordResetChallenge.builder()
                .id(UUID.randomUUID()).customerAccountId(account.getId()).email(account.getLoginEmail())
                .otpHash("otp").resetTokenHash(sha256("reset-token"))
                .verifiedAt(LocalDateTime.now().minusMinutes(1))
                .resetTokenExpiresAt(LocalDateTime.now().plusMinutes(5))
                .failedAttempts(0).expiresAt(LocalDateTime.now().plusMinutes(1))
                .createdAt(LocalDateTime.now()).build();
        when(challenges.findUnusedByResetTokenHashForUpdate(challenge.getResetTokenHash()))
                .thenReturn(Optional.of(challenge));
        when(accounts.findById(account.getId())).thenReturn(Optional.of(account));
        when(encoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest("reset-token", "new-password"));

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.getFailedLoginAttempts()).isZero();
        assertThat(account.getLoginLockedUntil()).isNull();
        assertThat(challenge.getUsedAt()).isNotNull();
        verify(refreshTokens).revokeAll(account.getId());
    }

    private CustomerAccount account(UUID customerId) {
        return CustomerAccount.builder().id(UUID.randomUUID()).customerId(customerId)
                .username("customer").loginEmail("customer@example.com")
                .passwordHash("old-hash").failedLoginAttempts(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

