package com.keni.doctorappointment.auth.dto;

import java.time.Instant;

/**
 * Token pair returned by {@code /api/auth/login} and {@code /api/auth/refresh}.
 *
 * @param accessToken  send as {@code Authorization: Bearer <accessToken>}
 * @param refreshToken send to {@code /api/auth/refresh} only
 * @param tokenType    always {@code Bearer}
 * @param expiresIn    access token lifetime in seconds
 */
public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		Long accountId,
		String email,
		String accountType,
		Instant issuedAt) {
}
