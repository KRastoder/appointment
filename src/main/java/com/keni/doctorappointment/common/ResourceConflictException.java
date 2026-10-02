package com.keni.doctorappointment.common;

/**
 * The request cannot be applied because of the current state of other data
 * (duplicate service name, deleting a user who still has appointments, ...).
 * Mapped to HTTP 409 CONFLICT.
 */
public class ResourceConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceConflictException(String message) {
		super(message);
	}

}
