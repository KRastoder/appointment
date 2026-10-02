package com.keni.doctorappointment.doctors.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class DoctorAvailabilityRequest {

	@NotNull(message = "dayOfWeek is required")
	@Min(value = 1, message = "dayOfWeek must be between 1 (Mon) and 7 (Sun)")
	@Max(value = 7, message = "dayOfWeek must be between 1 (Mon) and 7 (Sun)")
	int dayOfWeek;

	@NotNull(message = "startTime is required")
	java.time.LocalTime startTime;

	@NotNull(message = "endTime is required")
	java.time.LocalTime endTime;

}