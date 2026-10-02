package com.keni.doctorappointment.users;

/** Thrown when an e-mail is already taken by a patient, doctor or staff account. */
public class EmailAlreadyInUseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailAlreadyInUseException(String email) {
		super("E-mail %s is already in use".formatted(email));
	}

}
