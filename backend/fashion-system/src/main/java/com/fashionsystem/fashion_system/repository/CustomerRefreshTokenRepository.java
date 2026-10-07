package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerRefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRefreshTokenRepository extends BaseRepository<CustomerRefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from CustomerRefreshToken token where token.tokenHash=:tokenHash")
    Optional<CustomerRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update CustomerRefreshToken token set token.revokedAt=:revokedAt "
            + "where token.refreshTokenFamily=:family and token.revokedAt is null")
    int revokeFamily(@Param("family") UUID family, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("update CustomerRefreshToken token set token.revokedAt=:revokedAt "
            + "where token.customerAccountId=:accountId and token.revokedAt is null")
    int revokeAllForAccount(@Param("accountId") UUID accountId, @Param("revokedAt") LocalDateTime revokedAt);
}

