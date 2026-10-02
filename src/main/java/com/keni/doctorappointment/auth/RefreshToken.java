package com.keni.doctorappointment.auth;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * A refresh token, stored as a SHA-256 hash so that a database leak does not
 * hand out usable tokens.
 *
 * <p>Rotated on every refresh (the old row is revoked) and revoked on logout.
 * Access tokens stay stateless and cannot be revoked before they expire.</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_type", nullable = false, length = 20)
	private String accountType;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "issued_at", nullable = false, updatable = false)
	private OffsetDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "revoked_at")
	private OffsetDateTime revokedAt;

	public RefreshToken(AppUserPrincipal principal, String tokenHash, OffsetDateTime issuedAt,
			OffsetDateTime expiresAt) {
		this.accountType = principal.accountType().name();
		this.accountId = principal.id();
		this.tokenHash = tokenHash;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public boolean isActive(OffsetDateTime now) {
		return this.revokedAt == null && this.expiresAt.isAfter(now);
	}

}
