package com.keni.doctorappointment.users;

/**
 * Role of a {@link User} account.
 *
 * <p>{@link #PATIENT} books appointments, {@link #ADMIN} manages the service
 * catalogue and doctor accounts. Stored as {@code users.role} and mapped to
 * {@code ROLE_USER} / {@code ROLE_ADMIN} by the security layer.</p>
 */
public enum UserRole {

	PATIENT,
	ADMIN

}
