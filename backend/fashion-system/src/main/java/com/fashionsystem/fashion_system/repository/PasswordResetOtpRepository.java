package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy cập OTP quên mật khẩu. */
public interface PasswordResetOtpRepository extends BaseRepository<PasswordResetOtp, UUID> {
    Optional<PasswordResetOtp> findFirstByEmailAndAccountTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email, AccountType accountType);

    @Modifying
    @Query("""
            update PasswordResetOtp otp set otp.usedAt = :usedAt
             where otp.email = :email and otp.accountType = :accountType and otp.usedAt is null
            """)
    int markUnusedOtpsAsUsed(
            @Param("email") String email,
            @Param("accountType") AccountType accountType,
            @Param("usedAt") LocalDateTime usedAt);

    Optional<PasswordResetOtp> findFirstByResetTokenHashAndUsedAtIsNull(String resetTokenHash);
}
