package com.keni.doctorappointment.appointments;

/**
 * Thrown when an operation is not allowed in the current appointment status
 * (e.g. approving an appointment that is not {@code SCHEDULED}).
 *
 * <p>Mapped to HTTP 409 CONFLICT by
 * {@link com.keni.doctorappointment.config.ApiExceptionHandler}.</p>
 */
public class AppointmentStatusConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AppointmentStatusConflictException(String message) {
		super(message);
	}

}
