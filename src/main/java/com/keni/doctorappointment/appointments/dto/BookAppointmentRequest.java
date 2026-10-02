package com.keni.doctorappointment.appointments.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Request to book a new appointment. */
public record BookAppointmentRequest(

		@NotNull(message = "doctorId is required")
		Long doctorId,

		@NotNull(message = "serviceId is required")
		Long serviceId,

		@NotNull(message = "startTime is required")
		OffsetDateTime startTime,

		String notes) {

}