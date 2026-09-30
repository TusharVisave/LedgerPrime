package com.expensetracker.repository;

import com.expensetracker.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Bulk revoke bypasses the persistence context. {@code flushAutomatically}
     * writes any pending entity changes first (so e.g. a password change in
     * the same transaction is not lost), and {@code clearAutomatically}
     * evicts stale state afterwards: any RefreshToken already loaded would
     * otherwise keep {@code revoked=false} in memory and a later
     * {@code findByTokenHash} could wrongly treat a revoked token as active.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
