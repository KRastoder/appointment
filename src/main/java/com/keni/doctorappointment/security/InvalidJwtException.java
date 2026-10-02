package com.keni.doctorappointment.security;

/** Raised for a token that is malformed, forged, expired or otherwise unusable. */
public class InvalidJwtException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidJwtException(String message) {
		super(message);
	}

}
