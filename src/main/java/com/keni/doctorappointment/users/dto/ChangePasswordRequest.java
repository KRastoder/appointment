package com.keni.doctorappointment.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Password change body: the current password must be confirmed. */
public record ChangePasswordRequest(

		@NotBlank(message = "currentPassword is required")
		String currentPassword,

		@NotBlank(message = "newPassword is required")
		@Size(min = 8, max = 72, message = "password must be 8 to 72 characters long")
		@Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
		@Pattern(regexp = ".*[0-9].*", message = "password must contain a digit")
		String newPassword) {

}
