package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.PasswordResetProperties;
import com.fashionsystem.fashion_system.dto.auth.CustomerPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.CustomerVerifyPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpResponse;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerPasswordResetChallenge;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerPasswordResetChallengeRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Customer password reset")
@RequiredArgsConstructor
public class CustomerPasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String GENERIC = "Neu email ton tai, ma OTP dat lai mat khau da duoc gui.";
    private final CustomerAccountRepository accounts;
    private final CustomerRepository customers;
    private final CustomerPasswordResetChallengeRepository challenges;
    private final PasswordEncoder passwordEncoder;
    private final MailHelper mailHelper;
    private final CustomerRefreshTokenService refreshTokens;
    private final PasswordResetProperties properties;

    @Transactional
    public MessageResponse requestOtp(CustomerPasswordResetOtpRequest request) {
        String email = normalize(request.email());
        Optional<CustomerAccount> accountResult = accounts.findByLoginEmailIgnoreCase(email);
        if (accountResult.isEmpty()) return new MessageResponse(GENERIC);
        CustomerAccount account = accountResult.get();
        Optional<Customer> customerResult = customers.findById(account.getCustomerId())
                .filter(customer -> Boolean.TRUE.equals(customer.getActive()));
        if (customerResult.isEmpty()) return new MessageResponse(GENERIC);
        LocalDateTime now = LocalDateTime.now();
        enforceCooldown(email, now);
        challenges.markUnusedAsUsed(email, now);
        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        challenges.save(CustomerPasswordResetChallenge.builder()
                .customerAccountId(account.getId()).email(email).otpHash(passwordEncoder.encode(otp))
                .expiresAt(now.plusMinutes(properties.getCustomerOtpExpirationMinutes()))
                .failedAttempts(0).createdAt(now).build());
        try {
            mailHelper.sendOtp(email, customerResult.get().getFullName(), otp,
                    properties.getCustomerOtpExpirationMinutes());
        } catch (MailException exception) {
            throw BusinessException.serviceUnavailable("Khong the gui email luc nay", exception);
        }
        return new MessageResponse(GENERIC);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public VerifyPasswordResetOtpResponse verifyOtp(CustomerVerifyPasswordResetOtpRequest request) {
        CustomerPasswordResetChallenge stored = challenges.findLatestUnusedForUpdate(normalize(request.email()))
                .orElseThrow(this::invalidOtp);
        LocalDateTime now = LocalDateTime.now();
        if (stored.getVerifiedAt() != null || !stored.getExpiresAt().isAfter(now)
                || stored.getFailedAttempts() >= properties.getMaxOtpAttempts()) {
            stored.setUsedAt(now);
            challenges.save(stored);
            throw invalidOtp();
        }
        if (!passwordEncoder.matches(request.otp(), stored.getOtpHash())) {
            stored.setFailedAttempts(stored.getFailedAttempts() + 1);
            if (stored.getFailedAttempts() >= properties.getMaxOtpAttempts()) stored.setUsedAt(now);
            challenges.save(stored);
            throw invalidOtp();
        }
        String token = randomToken();
        stored.setVerifiedAt(now);
        stored.setResetTokenHash(hash(token));
        stored.setResetTokenExpiresAt(now.plusMinutes(properties.getResetTokenExpirationMinutes()));
        challenges.save(stored);
        return new VerifyPasswordResetOtpResponse(
                token, properties.getResetTokenExpirationMinutes() * 60);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        CustomerPasswordResetChallenge stored = challenges
                .findUnusedByResetTokenHashForUpdate(hash(request.resetToken()))
                .orElseThrow(this::invalidToken);
        LocalDateTime now = LocalDateTime.now();
        if (stored.getVerifiedAt() == null || stored.getResetTokenExpiresAt() == null
                || !stored.getResetTokenExpiresAt().isAfter(now)) throw invalidToken();
        CustomerAccount account = accounts.findById(stored.getCustomerAccountId())
                .orElseThrow(this::invalidToken);
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.setFailedLoginAttempts(0);
        account.setLoginLockedUntil(null);
        account.setUpdatedAt(now);
        accounts.save(account);
        stored.setUsedAt(now);
        challenges.save(stored);
        refreshTokens.revokeAll(account.getId());
        return new MessageResponse("Mat khau da duoc cap nhat thanh cong.");
    }

    private void enforceCooldown(String email, LocalDateTime now) {
        long cooldown = Math.max(1, properties.getResendCooldownSeconds());
        challenges.findFirstByEmailOrderByCreatedAtDesc(email)
                .filter(value -> value.getCreatedAt().plusSeconds(cooldown).isAfter(now))
                .ifPresent(value -> {
                    long millis = Duration.between(now, value.getCreatedAt().plusSeconds(cooldown)).toMillis();
                    throw BusinessException.tooManyRequests(
                            "Vui lòng chờ trước khi gửi lại OTP.", Math.max(1, (millis + 999) / 1000));
                });
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String randomToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }
    private BusinessException invalidOtp() { return BusinessException.badRequest("OTP khong hop le hoac da het han"); }
    private BusinessException invalidToken() { return BusinessException.badRequest("Reset token khong hop le hoac da het han"); }
}

