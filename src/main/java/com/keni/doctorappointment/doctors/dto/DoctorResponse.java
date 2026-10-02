package com.keni.doctorappointment.doctors.dto;

import java.time.OffsetDateTime;

import com.keni.doctorappointment.doctors.Doctor;

/** Outbound representation of a {@link Doctor}. */
public record DoctorResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phoneNumber,
		String specialization,
		String bio,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static DoctorResponse from(Doctor doctor) {
		return new DoctorResponse(
				doctor.getId(),
				doctor.getFirstName(),
				doctor.getLastName(),
				doctor.getEmail(),
				doctor.getPhoneNumber(),
				doctor.getSpecialization(),
				doctor.getBio(),
				doctor.getCreatedAt(),
				doctor.getUpdatedAt());
	}

}
