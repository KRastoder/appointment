package com.keni.doctorappointment.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings, bound from {@code app.security.jwt.*}.
 *
 * <p>Provided by environment variable {@code JWT_SECRET} (see
 * {@code .env.example}); the value in {@code application.yml} is a
 * development default only.</p>
 *
 * @param secret        HMAC key, at least 32 bytes for HS256
 * @param issuer        value of the {@code iss} claim
 * @param accessTokenTtl lifetime of an access token
 * @param refreshTokenTtl lifetime of a refresh token
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

	public JwtProperties {
		if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
					"app.security.jwt.secret must be at least 32 bytes long (set the JWT_SECRET env variable)");
		}
		issuer = issuer != null ? issuer : "doctor-appointment";
		accessTokenTtl = accessTokenTtl != null ? accessTokenTtl : Duration.ofMinutes(30);
		refreshTokenTtl = refreshTokenTtl != null ? refreshTokenTtl : Duration.ofDays(14);
	}

}
