package com.keni.doctorappointment.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies the signature and puts
 * an {@link AppUserPrincipal} into the security context.
 *
 * <p>Replaces the HTTP Basic filter: there is no session, every request
 * carries its own token.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenService jwtTokenService;

	private final DeletedAccountRegistry deletedAccounts;

	public JwtAuthenticationFilter(JwtTokenService jwtTokenService, DeletedAccountRegistry deletedAccounts) {
		this.jwtTokenService = jwtTokenService;
		this.deletedAccounts = deletedAccounts;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String token = resolveToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				Map<String, Object> claims = jwtTokenService.parse(token);
				if (JwtTokenService.TYPE_ACCESS.equals(claims.get(JwtTokenService.CLAIM_TOKEN_TYPE))) {
					if (isDeletedAccount(claims)) {
						log.debug("Rejected bearer token of a deleted account");
					}
					else {
						SecurityContextHolder.getContext()
							.setAuthentication(toAuthentication(claims));
					}
				}
				else {
					log.debug("Ignoring refresh token used as access token");
				}
			}
			catch (InvalidJwtException ex) {
				// No authentication -> the entry point answers 401 for protected paths.
				log.debug("Rejected bearer token: {}", ex.getMessage());
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * An access token stays valid for its whole lifetime even if the account is
	 * deleted meanwhile; {@link DeletedAccountRegistry} remembers the deletion
	 * so such a token can be told apart from one of a re-created account.
	 */
	private boolean isDeletedAccount(Map<String, Object> claims) {
		long accountId = JwtTokenService.asLong(claims.get("account_id"));
		AppUserPrincipal.AccountType accountType = AppUserPrincipal.AccountType
			.valueOf(String.valueOf(claims.get("account_type")));
		Instant issuedAt = issuedAt(claims);
		return deletedAccounts.isInvalidated(accountType, accountId, issuedAt);
	}

	private Instant issuedAt(Map<String, Object> claims) {
		Object iat = claims.get(JwtTokenService.CLAIM_ISSUED_AT);
		return iat instanceof Number seconds ? Instant.ofEpochSecond(seconds.longValue()) : null;
	}

	private UsernamePasswordAuthenticationToken toAuthentication(Map<String, Object> claims) {
		String email = String.valueOf(claims.get("sub"));
		long id = JwtTokenService.asLong(claims.get("account_id"));
		AppUserPrincipal.AccountType accountType = AppUserPrincipal.AccountType
			.valueOf(String.valueOf(claims.get("account_type")));

		List<SimpleGrantedAuthority> authorities = JwtTokenService.stringList(claims.get(JwtTokenService.CLAIM_ROLES))
			.stream()
			.map(String::valueOf)
			.filter(authority -> !authority.isBlank())
			.map(authority -> new SimpleGrantedAuthority(
					authority.startsWith("ROLE_") ? authority : "ROLE_" + authority))
			.toList();

		AppUserPrincipal principal = new AppUserPrincipal(id, email, null, accountType,
				(Collection<SimpleGrantedAuthority>) (Collection<?>) authorities);

		return new UsernamePasswordAuthenticationToken(principal, null, authorities);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length()).trim();
			return token.isEmpty() ? null : token;
		}
		return null;
	}

}
