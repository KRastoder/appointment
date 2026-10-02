package com.keni.doctorappointment.doctors.availability.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class DoctorTimeOffRequest {

	@NotNull(message = "startAt is required")
	java.time.OffsetDateTime startAt;

	@NotNull(message = "endAt is required")
	java.time.OffsetDateTime endAt;

	String reason;

}