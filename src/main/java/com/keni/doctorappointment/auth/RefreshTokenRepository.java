package com.keni.doctorappointment.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for refresh tokens. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByAccountTypeAndAccountId(String accountType, Long accountId);

	@Modifying
	@Query("""
			update RefreshToken t
			   set t.revokedAt = :now
			 where t.accountType = :accountType
			   and t.accountId = :accountId
			   and t.revokedAt is null
			""")
	int revokeAllForAccount(@Param("accountType") String accountType, @Param("accountId") Long accountId,
			@Param("now") OffsetDateTime now);

	@Modifying
	@Query("""
			update RefreshToken t
			   set t.revokedAt = :now
			 where t.expiresAt < :threshold
			   and t.revokedAt is null
			""")
	int deleteExpiredBefore(@Param("threshold") OffsetDateTime threshold);

}
