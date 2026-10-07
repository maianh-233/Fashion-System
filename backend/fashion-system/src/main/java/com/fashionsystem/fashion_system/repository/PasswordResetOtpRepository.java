package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/** Truy cập OTP quên mật khẩu. */
public interface PasswordResetOtpRepository extends BaseRepository<PasswordResetOtp, UUID> {
    Optional<PasswordResetOtp> findFirstByEmailAndAccountTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email, AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select otp from PasswordResetOtp otp
             where otp.email = :email and otp.accountType = :accountType and otp.usedAt is null
             order by otp.createdAt desc
            """)
    Optional<PasswordResetOtp> findLatestUnusedForUpdate(
            @Param("email") String email, @Param("accountType") AccountType accountType);

    Optional<PasswordResetOtp> findFirstByEmailAndAccountTypeOrderByCreatedAtDesc(
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select otp from PasswordResetOtp otp
             where otp.resetTokenHash = :resetTokenHash and otp.usedAt is null
            """)
    Optional<PasswordResetOtp> findUnusedByResetTokenHashForUpdate(
            @Param("resetTokenHash") String resetTokenHash);
}
