package com.keni.doctorappointment.doctors.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Doctor profile update body. All fields optional; {@code null} keeps the
 * current value. The password is not changed here.
 */
public record UpdateDoctorRequest(

		@Size(max = 100, message = "firstName must not exceed 100 characters")
		String firstName,

		@Size(max = 100, message = "lastName must not exceed 100 characters")
		String lastName,

		@Email(message = "email is not valid")
		@Size(max = 320, message = "email must not exceed 320 characters")
		String email,

		@Size(max = 32, message = "phoneNumber must not exceed 32 characters")
		String phoneNumber,

		@Size(max = 150, message = "specialization must not exceed 150 characters")
		String specialization,

		String bio) {

}
