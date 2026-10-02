package com.keni.doctorappointment.users.dto;

import com.keni.doctorappointment.users.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Account update body. All fields optional; {@code null} means "keep the
 * current value". The password is changed through the dedicated password
 * endpoint, never here.
 */
public record UpdateUserRequest(

		@Size(max = 100, message = "firstName must not exceed 100 characters")
		String firstName,

		@Size(max = 100, message = "lastName must not exceed 100 characters")
		String lastName,

		@Email(message = "email is not valid")
		@Size(max = 320, message = "email must not exceed 320 characters")
		String email,

		@Size(max = 32, message = "phoneNumber must not exceed 32 characters")
		String phoneNumber,

		/** Role change: ADMIN only. */
		UserRole role) {

}
