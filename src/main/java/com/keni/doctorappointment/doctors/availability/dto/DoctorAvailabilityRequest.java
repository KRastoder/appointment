package com.keni.doctorappointment.doctors.availability.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Weekly working-hours entry. Day of week: 1 = Monday .. 7 = Sunday. */
public record DoctorAvailabilityRequest(

		@NotNull(message = "dayOfWeek is required")
		@Min(value = 1, message = "dayOfWeek must be between 1 (Mon) and 7 (Sun)")
		@Max(value = 7, message = "dayOfWeek must be between 1 (Mon) and 7 (Sun)")
		int dayOfWeek,

		@NotNull(message = "startTime is required")
		LocalTime startTime,

		@NotNull(message = "endTime is required")
		LocalTime endTime) {

}