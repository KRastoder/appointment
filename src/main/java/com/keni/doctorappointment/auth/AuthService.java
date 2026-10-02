package com.keni.doctorappointment.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.auth.dto.AuthResponse;
import com.keni.doctorappointment.config.JwtProperties;
import com.keni.doctorappointment.security.AppUserDetailsService;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.security.InvalidJwtException;
import com.keni.doctorappointment.security.JwtTokenService;

/**
 * Login, refresh and logout.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>login: e-mail + BCrypt password check, then an access/refresh pair is
 *       issued; the refresh token hash is stored</li>
 *   <li>refresh: the refresh token is verified, looked up in the database,
 *       revoked (rotation) and replaced by a fresh pair</li>
 *   <li>logout: the refresh token is revoked; the access token simply expires
 *       (stateless, no blacklist)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AppUserDetailsService userDetailsService;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenService jwtTokenService;

	private final RefreshTokenRepository refreshTokenRepository;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	@Transactional
	public AuthResponse login(String email, String rawPassword) {
		String login = normaliseEmail(email);

		UserDetails details;
		try {
			details = userDetailsService.loadUserByUsername(login);
		}
		catch (AuthenticationException ex) {
			// Same answer for unknown e-mail and wrong password.
			throw new BadCredentialsException("Invalid e-mail or password");
		}

		AppUserPrincipal principal = (AppUserPrincipal) details;
		if (!passwordEncoder.matches(rawPassword, principal.getPassword())) {
			throw new BadCredentialsException("Invalid e-mail or password");
		}

		return issuePair(principal);
	}

	@Transactional
	public AuthResponse refresh(String refreshToken) {
		Map<String, Object> claims;
		try {
			claims = jwtTokenService.parse(refreshToken);
		}
		catch (InvalidJwtException ex) {
			throw new BadCredentialsException("Invalid refresh token");
		}
		if (!JwtTokenService.TYPE_REFRESH.equals(claims.get(JwtTokenService.CLAIM_TOKEN_TYPE))) {
			throw new BadCredentialsException("Not a refresh token");
		}

		RefreshToken stored = refreshTokenRepository.findByTokenHash(jwtTokenService.hash(refreshToken))
			.filter(token -> token.isActive(now()))
			.orElseThrow(() -> new BadCredentialsException("Refresh token is revoked or expired"));

		// Rotation: the presented token can not be used again.
		stored.setRevokedAt(now());

		AppUserPrincipal principal = (AppUserPrincipal) userDetailsService
			.loadUserByUsername(String.valueOf(claims.get("sub")));
		return issuePair(principal);
	}

	@Transactional
	public void logout(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(jwtTokenService.hash(refreshToken))
			.filter(token -> token.getRevokedAt() == null)
			.ifPresent(token -> token.setRevokedAt(now()));
	}

	private AuthResponse issuePair(AppUserPrincipal principal) {
		Instant issuedAt = now().toInstant();

		String accessToken = jwtTokenService.createAccessToken(principal, issuedAt);
		String refreshToken = jwtTokenService.createRefreshToken(principal, issuedAt);

		refreshTokenRepository.save(new RefreshToken(principal, jwtTokenService.hash(refreshToken),
				issuedAt.atOffset(ZoneOffset.UTC),
				jwtTokenService.expiresAtOf(jwtTokenService.parse(refreshToken)).atOffset(ZoneOffset.UTC)));

		return new AuthResponse(accessToken, refreshToken, "Bearer", jwtProperties.accessTokenTtl().toSeconds(),
				principal.id(), principal.getUsername(), principal.accountType().name(), issuedAt);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(clock);
	}

	static String normaliseEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
	}

}
