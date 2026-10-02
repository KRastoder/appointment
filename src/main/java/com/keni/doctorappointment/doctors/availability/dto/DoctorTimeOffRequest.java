package com.keni.doctorappointment.doctors.availability.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

/** Blocked period (holiday, sick leave, ...) of a doctor. */
public record DoctorTimeOffRequest(

		@NotNull(message = "startAt is required")
		OffsetDateTime startAt,

		@NotNull(message = "endAt is required")
		OffsetDateTime endAt,

		String reason) {

}