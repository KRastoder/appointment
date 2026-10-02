package com.keni.doctorappointment.appointments.dto;

import jakarta.validation.constraints.NotNull;

/** Request to reschedule an appointment. */
public record RescheduleAppointmentRequest(

		@NotNull(message = "newStartTime is required")
		java.time.OffsetDateTime newStartTime,

		String reason) {

}