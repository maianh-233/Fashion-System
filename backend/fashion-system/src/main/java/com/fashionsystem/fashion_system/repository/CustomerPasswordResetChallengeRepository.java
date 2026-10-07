package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerPasswordResetChallenge;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerPasswordResetChallengeRepository
        extends BaseRepository<CustomerPasswordResetChallenge, UUID> {
    Optional<CustomerPasswordResetChallenge> findFirstByEmailOrderByCreatedAtDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerPasswordResetChallenge c where c.email=:email and c.usedAt is null order by c.createdAt desc")
    Optional<CustomerPasswordResetChallenge> findLatestUnusedForUpdate(@Param("email") String email);

    @Modifying
    @Query("update CustomerPasswordResetChallenge c set c.usedAt=:usedAt where c.email=:email and c.usedAt is null")
    int markUnusedAsUsed(@Param("email") String email, @Param("usedAt") LocalDateTime usedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerPasswordResetChallenge c where c.resetTokenHash=:hash and c.usedAt is null")
    Optional<CustomerPasswordResetChallenge> findUnusedByResetTokenHashForUpdate(@Param("hash") String hash);
}

