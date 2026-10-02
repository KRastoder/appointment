package com.keni.doctorappointment.appointments;

/**
 * Lifecycle status of an appointment.
 *
 * <p>Persisted as {@code VARCHAR} via {@code @Enumerated(STRING)} and
 * additionally constrained in the database by
 * {@code ck_appointments_status}.</p>
 *
 * <p>Transition rules (who may move an appointment from which state to
 * which, and when) are business logic and are NOT implemented yet.</p>
 */
public enum AppointmentStatus {

	SCHEDULED,
	CONFIRMED,
	CANCELLED,
	COMPLETED,
	NO_SHOW

}
