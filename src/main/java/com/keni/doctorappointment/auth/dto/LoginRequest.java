package com.keni.doctorappointment.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/auth/login} body. */
public record LoginRequest(

		@NotBlank(message = "email is required")
		@Email(message = "email is not valid")
		String email,

		@NotBlank(message = "password is required")
		String password) {

}
