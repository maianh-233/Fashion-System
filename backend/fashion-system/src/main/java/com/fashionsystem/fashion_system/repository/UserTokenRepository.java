package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.UserToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho UserToken.
 */
public interface UserTokenRepository extends BaseRepository<UserToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from UserToken token where token.tokenHash = :tokenHash")
    Optional<UserToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update UserToken token set token.revokedAt = :revokedAt "
            + "where token.refreshTokenFamily = :family and token.revokedAt is null")
    int revokeFamily(@Param("family") UUID family, @Param("revokedAt") LocalDateTime revokedAt);
}
