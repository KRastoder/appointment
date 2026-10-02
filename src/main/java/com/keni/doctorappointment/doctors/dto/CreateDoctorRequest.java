package com.keni.doctorappointment.doctors.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Doctor account creation body (ADMIN only).
 *
 * <p>Doctor self registration is deliberately not offered: a doctor can approve
 * appointments, so accounts must be created by staff.</p>
 */
public record CreateDoctorRequest(

		@NotBlank(message = "firstName is required")
		@Size(max = 100, message = "firstName must not exceed 100 characters")
		String firstName,

		@NotBlank(message = "lastName is required")
		@Size(max = 100, message = "lastName must not exceed 100 characters")
		String lastName,

		@NotBlank(message = "email is required")
		@Email(message = "email is not valid")
		@Size(max = 320, message = "email must not exceed 320 characters")
		String email,

		@NotBlank(message = "password is required")
		@Size(min = 8, max = 72, message = "password must be 8 to 72 characters long")
		@Pattern(regexp = ".*[A-Za-z].*", message = "password must contain a letter")
		@Pattern(regexp = ".*[0-9].*", message = "password must contain a digit")
		String password,

		@Size(max = 32, message = "phoneNumber must not exceed 32 characters")
		String phoneNumber,

		@NotBlank(message = "specialization is required")
		@Size(max = 150, message = "specialization must not exceed 150 characters")
		String specialization,

		String bio) {

}
