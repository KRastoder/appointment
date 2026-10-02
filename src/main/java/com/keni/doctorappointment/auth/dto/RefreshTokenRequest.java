package com.keni.doctorappointment.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/auth/refresh} body. */
public record RefreshTokenRequest(@NotBlank(message = "refreshToken is required") String refreshToken) {

}
