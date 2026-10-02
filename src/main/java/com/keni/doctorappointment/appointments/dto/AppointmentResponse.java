package com.keni.doctorappointment.appointments.dto;

import java.time.OffsetDateTime;

import com.keni.doctorappointment.appointments.Appointment;
import com.keni.doctorappointment.appointments.AppointmentStatus;

/**
 * Outbound representation of an {@link Appointment}.
 *
 * <p>Associations are exposed as ids so that serialising an appointment never
 * triggers lazy loading of user/doctor/service.</p>
 */
public record AppointmentResponse(
		Long id,
		Long userId,
		Long doctorId,
		Long serviceId,
		OffsetDateTime startTime,
		OffsetDateTime endTime,
		AppointmentStatus status,
		String notes,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static AppointmentResponse from(Appointment appointment) {
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getUser() != null ? appointment.getUser().getId() : null,
				appointment.getDoctor() != null ? appointment.getDoctor().getId() : null,
				appointment.getService() != null ? appointment.getService().getId() : null,
				appointment.getStartTime(),
				appointment.getEndTime(),
				appointment.getStatus(),
				appointment.getNotes(),
				appointment.getCreatedAt(),
				appointment.getUpdatedAt());
	}

}
