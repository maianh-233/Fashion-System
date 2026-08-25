package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.PasswordResetProperties;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.PasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpResponse;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.PasswordResetOtpRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Điều phối gửi OTP, xác minh OTP và đổi mật khẩu cho employee/customer. */
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_SENT_MESSAGE =
            "Neu email ton tai, ma OTP dat lai mat khau da duoc gui.";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailHelper mailHelper;

    private final PasswordResetProperties properties;

    /** Tạo OTP mới. Endpoint gửi lại OTP cũng gọi cùng phương thức này. */
    @Transactional
    public MessageResponse requestOtp(PasswordResetOtpRequest request) {
        String email = normalizeEmail(request.email());
        Optional<Account> account = findActiveAccount(email, request.accountType());
        if (account.isEmpty()) {
            return new MessageResponse(GENERIC_SENT_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now();
        invalidateOldOtps(email, request.accountType(), now);
        String otp = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        Account target = account.get();
        otpRepository.save(prepareOtp(request.accountType(), target, email, otp, now));
        sendOtp(target, email, otp);
        return new MessageResponse(GENERIC_SENT_MESSAGE);
    }

    /** Xác minh OTP và cấp reset token ngẫu nhiên có thời hạn ngắn. */
    @Transactional(noRollbackFor = BusinessException.class)
    public VerifyPasswordResetOtpResponse verifyOtp(VerifyPasswordResetOtpRequest request) {
        String email = normalizeEmail(request.email());
        PasswordResetOtp stored = otpRepository
                .findFirstByEmailAndAccountTypeAndUsedAtIsNullOrderByCreatedAtDesc(email, request.accountType())
                .orElseThrow(this::invalidOtp);
        LocalDateTime now = LocalDateTime.now();
        validateOtpCanBeVerified(stored, now);
        if (!passwordEncoder.matches(request.otp(), stored.getOtpHash())) {
            recordFailedAttempt(stored, now);
            throw invalidOtp();
        }

        String resetToken = generateResetToken();
        markVerified(stored, resetToken, now);
        return new VerifyPasswordResetOtpResponse(
                resetToken, properties.getResetTokenExpirationMinutes() * 60);
    }

    /** Đổi mật khẩu bằng reset token; token bị vô hiệu hóa ngay sau lần dùng đầu tiên. */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp stored = otpRepository
                .findFirstByResetTokenHashAndUsedAtIsNull(hashToken(request.resetToken()))
                .orElseThrow(this::invalidResetToken);
        LocalDateTime now = LocalDateTime.now();
        validateResetToken(stored, now);
        updatePassword(stored, passwordEncoder.encode(request.newPassword()), now);
        stored.setUsedAt(now);
        otpRepository.save(stored);
        return new MessageResponse("Mat khau da duoc cap nhat thanh cong.");
    }

    private Optional<Account> findActiveAccount(String email, AccountType type) {
        if (type == AccountType.EMPLOYEE) {
            return userRepository.findByEmail(email)
                    .filter(user -> Boolean.TRUE.equals(user.getActive()) && user.getDeletedAt() == null)
                    .map(user -> new Account(user.getId(), user.getFullName()));
        }
        return customerRepository.findByEmail(email)
                .filter(customer -> Boolean.TRUE.equals(customer.getActive()))
                .map(customer -> new Account(customer.getId(), customer.getFullName()));
    }

    private void invalidateOldOtps(String email, AccountType accountType, LocalDateTime now) {
        otpRepository.markUnusedOtpsAsUsed(email, accountType, now);
    }

    private PasswordResetOtp prepareOtp(
            AccountType accountType, Account account, String email, String rawOtp, LocalDateTime now) {
        return PasswordResetOtp.builder()
                .accountType(accountType)
                .accountId(account.id())
                .email(email)
                .otpHash(passwordEncoder.encode(rawOtp))
                .expiresAt(now.plusMinutes(properties.getOtpExpirationMinutes()))
                .failedAttempts(0)
                .createdAt(now)
                .build();
    }

    private void sendOtp(Account account, String email, String otp) {
        try {
            mailHelper.sendOtp(email, account.displayName(), otp, properties.getOtpExpirationMinutes());
        } catch (MailException exception) {
            throw BusinessException.serviceUnavailable("Khong the gui email luc nay", exception);
        }
    }

    private void validateOtpCanBeVerified(PasswordResetOtp stored, LocalDateTime now) {
        if (stored.getVerifiedAt() != null || !stored.getExpiresAt().isAfter(now)
                || stored.getFailedAttempts() >= properties.getMaxOtpAttempts()) {
            stored.setUsedAt(now);
            otpRepository.save(stored);
            throw invalidOtp();
        }
    }

    private void recordFailedAttempt(PasswordResetOtp stored, LocalDateTime now) {
        int failedAttempts = stored.getFailedAttempts() + 1;
        stored.setFailedAttempts(failedAttempts);
        if (failedAttempts >= properties.getMaxOtpAttempts()) stored.setUsedAt(now);
        otpRepository.save(stored);
    }

    private void markVerified(PasswordResetOtp stored, String resetToken, LocalDateTime now) {
        stored.setVerifiedAt(now);
        stored.setResetTokenHash(hashToken(resetToken));
        stored.setResetTokenExpiresAt(now.plusMinutes(properties.getResetTokenExpirationMinutes()));
        otpRepository.save(stored);
    }

    private void validateResetToken(PasswordResetOtp stored, LocalDateTime now) {
        if (stored.getVerifiedAt() == null || stored.getResetTokenExpiresAt() == null
                || !stored.getResetTokenExpiresAt().isAfter(now)) {
            throw invalidResetToken();
        }
    }

    private void updatePassword(PasswordResetOtp stored, String passwordHash, LocalDateTime now) {
        if (stored.getAccountType() == AccountType.EMPLOYEE) {
            updateEmployeePassword(stored, passwordHash, now);
            return;
        }
        Customer customer = customerRepository.findById(stored.getAccountId())
                .orElseThrow(this::invalidResetToken);
        customer.setPasswordHash(passwordHash);
        customer.setUpdatedAt(now);
        customer.setLocked(false);
        customerRepository.save(customer);
    }

    private void updateEmployeePassword(PasswordResetOtp stored, String passwordHash, LocalDateTime now) {
        User user = userRepository.findById(stored.getAccountId()).orElseThrow(this::invalidResetToken);
        user.setPasswordHash(passwordHash);
        user.setLastPasswordChange(now);
        user.setUpdatedAt(now);
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private BusinessException invalidOtp() {
        return BusinessException.badRequest("OTP khong hop le hoac da het han");
    }

    private BusinessException invalidResetToken() {
        return BusinessException.badRequest("Reset token khong hop le hoac da het han");
    }

    private record Account(UUID id, String displayName) {
    }
}
