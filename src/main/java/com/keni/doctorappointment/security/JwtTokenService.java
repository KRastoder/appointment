package com.keni.doctorappointment.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import com.keni.doctorappointment.config.JwtProperties;

/**
 * Minimal HS256 JSON Web Tokens, implemented directly with
 * {@link Mac} - no OAuth2 / JOSE / Nimbus libraries involved.
 *
 * <p>Layout: {@code base64url(header) + "." + base64url(claims) + "." +
 * base64url(HMAC-SHA256(payload))}. The signature is always verified before a
 * token is trusted.</p>
 *
 * <p>Two token kinds are issued, distinguished by the {@code typ} claim:</p>
 * <ul>
 *   <li><b>access</b> - short lived, sent as {@code Authorization: Bearer ...},
 *       carries the account id, type and roles</li>
 *   <li><b>refresh</b> - long lived, only accepted by
 *       {@code /api/auth/refresh} and stored hashed in the database so that it
 *       can be revoked (logout)</li>
 * </ul>
 */
@Component
public class JwtTokenService {

	static final String CLAIM_ROLES = "roles";

	static final String CLAIM_TYPE = "typ";

	public static final String CLAIM_TOKEN_TYPE = "token_type";

	static final String TYPE_ACCESS = "access";

	public static final String TYPE_REFRESH = "refresh";

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

	private final ObjectMapper objectMapper;

	private final JwtProperties properties;

	private final byte[] keyBytes;

	public JwtTokenService(ObjectMapper objectMapper, JwtProperties properties) {
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
	}

	/** Issues a signed access token carrying identity and roles. */
	public String createAccessToken(AppUserPrincipal principal, Instant issuedAt) {
		Map<String, Object> claims = baseClaims(principal, issuedAt, TYPE_ACCESS);
		claims.put(CLAIM_ROLES, principal.roles());
		claims.put("exp", issuedAt.plus(properties.accessTokenTtl()).getEpochSecond());
		return sign(claims);
	}

	/** Issues a signed refresh token (no roles: it only buys new tokens). */
	public String createRefreshToken(AppUserPrincipal principal, Instant issuedAt) {
		Map<String, Object> claims = baseClaims(principal, issuedAt, TYPE_REFRESH);
		claims.put("exp", issuedAt.plus(properties.refreshTokenTtl()).getEpochSecond());
		return sign(claims);
	}

	private Map<String, Object> baseClaims(AppUserPrincipal principal, Instant issuedAt, String tokenType) {
		Map<String, Object> claims = new java.util.LinkedHashMap<>();
		claims.put("sub", principal.getUsername());
		claims.put("iss", properties.issuer());
		claims.put("account_id", principal.id());
		claims.put("account_type", principal.accountType().name());
		claims.put(CLAIM_TOKEN_TYPE, tokenType);
		// Two tokens for the same account issued in the same second must differ.
		claims.put("jti", java.util.UUID.randomUUID().toString());
		claims.put("iat", issuedAt.getEpochSecond());
		return claims;
	}

	/**
	 * Verifies signature, issuer and expiry.
	 *
	 * @return the token claims
	 * @throws InvalidJwtException if the token is malformed, forged or expired
	 */
	public Map<String, Object> parse(String token) {
		if (token == null || token.isBlank()) {
			throw new InvalidJwtException("Missing token");
		}
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new InvalidJwtException("Malformed token");
		}

		byte[] expected = hmac((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
		byte[] actual;
		try {
			actual = BASE64_URL_DECODER.decode(parts[2]);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidJwtException("Malformed signature");
		}
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new InvalidJwtException("Invalid signature");
		}

		Map<String, Object> claims;
		try {
			claims = this.objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), Map.class);
		}
		catch (RuntimeException ex) {
			throw new InvalidJwtException("Malformed payload");
		}

		if (!properties.issuer().equals(claims.get("iss"))) {
			throw new InvalidJwtException("Unexpected issuer");
		}
		Instant expiresAt = Instant.ofEpochSecond(asLong(claims.get("exp")));
		if (!expiresAt.isAfter(Instant.now())) {
			throw new InvalidJwtException("Token expired");
		}
		return claims;
	}

	/** SHA-256 hex digest: refresh tokens are stored hashed, never in clear. */
	public String hash(String token) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	public Instant expiresAtOf(Map<String, Object> claims) {
		return Instant.ofEpochSecond(asLong(claims.get("exp")));
	}

	Instant issuedAtOf(Map<String, Object> claims) {
		return Instant.ofEpochSecond(asLong(claims.get("iat")));
	}

	// --- internals ------------------------------------------------------

	private String sign(Map<String, Object> claims) {
		String header = BASE64_URL.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
		String payload;
		try {
			payload = BASE64_URL.encodeToString(objectMapper.writeValueAsString(claims)
				.getBytes(StandardCharsets.UTF_8));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Could not serialise token claims", ex);
		}
		String signingInput = header + "." + payload;
		return signingInput + "." + BASE64_URL.encodeToString(hmac(signingInput.getBytes(StandardCharsets.UTF_8)));
	}

	private byte[] hmac(byte[] data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(this.keyBytes, HMAC_ALGORITHM));
			return mac.doFinal(data);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not sign token", ex);
		}
	}

	static long asLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		throw new InvalidJwtException("Unexpected claim type");
	}

	@SuppressWarnings("unchecked")
	static List<String> stringList(Object value) {
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return List.of();
	}

}
