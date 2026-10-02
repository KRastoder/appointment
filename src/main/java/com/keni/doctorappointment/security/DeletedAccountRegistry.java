package com.keni.doctorappointment.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Remembers deleted accounts so that access tokens issued before the deletion
 * stop working immediately.
 *
 * <p>An access token is self-contained: {@link JwtAuthenticationFilter} trusts
 * its claims and never looks at the database, so deleting a {@code users} or
 * {@code doctors} row would otherwise leave the account usable until the token
 * expires (30 minutes by default). Refresh tokens are already revoked in the
 * database; this registry closes the gap for access tokens.</p>
 *
 * <p>Each entry records <em>when</em> the account was deleted. A token issued
 * before that moment is rejected, a token issued afterwards (the account was
 * re-created) is accepted. Entries are dropped once they are older than the
 * access token lifetime, because by then every token they could affect has
 * expired anyway.</p>
 *
 * <p>State is per JVM instance and in memory: a restart clears it, and in a
 * multi-instance deployment the other instances would not see a deletion until
 * their entries expire. Persisting the deletion timestamp on the account row
 * (and comparing it against the {@code iat} claim) is the durable variant.</p>
 */
@Component
public class DeletedAccountRegistry {

	private final Map<String, Instant> deletedAt = new ConcurrentHashMap<>();

	private final Duration accessTokenTtl;

	public DeletedAccountRegistry(@Value("${app.security.jwt.access-token-ttl:30m}") Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	/** Records the deletion of an account, invalidating its existing tokens. */
	public void invalidate(AppUserPrincipal.AccountType accountType, long accountId) {
		purgeExpired();
		deletedAt.put(key(accountType, accountId), Instant.now());
	}

	/**
	 * @param issuedAt the {@code iat} claim of the presented token
	 * @return {@code true} if the token was issued before the account was deleted
	 */
	public boolean isInvalidated(AppUserPrincipal.AccountType accountType, long accountId, Instant issuedAt) {
		Instant deleted = deletedAt.get(key(accountType, accountId));
		return deleted != null && issuedAt != null && !issuedAt.isAfter(deleted);
	}

	private void purgeExpired() {
		Instant cutoff = Instant.now().minus(accessTokenTtl);
		deletedAt.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
	}

	private String key(AppUserPrincipal.AccountType accountType, long accountId) {
		return accountType.name() + ':' + accountId;
	}

}