package com.keni.doctorappointment.config;

import java.util.NoSuchElementException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.keni.doctorappointment.appointments.AppointmentStatusConflictException;
import com.keni.doctorappointment.common.ResourceConflictException;
import com.keni.doctorappointment.users.EmailAlreadyInUseException;

/**
 * Error mapping shared by all modules.
 *
 * <p>Technical failures and the authorisation rules are translated here
 * (404 / 409 / 400 / 401). Business rules that reject an otherwise valid
 * request - a start time in the past, a service the doctor does not offer, a
 * slot outside his working hours - are signalled with
 * {@link IllegalArgumentException} by the service layer and answered as 400;
 * rules that clash with existing data (duplicate rating, e-mail already in use)
 * use {@link ResourceConflictException} and are answered as 409.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NoSuchElementException.class)
	public ProblemDetail handleNotFound(NoSuchElementException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Not found");
		return problem;
	}

	@ExceptionHandler({ AppointmentStatusConflictException.class, EmailAlreadyInUseException.class,
			ResourceConflictException.class })
	public ProblemDetail handleConflict(RuntimeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
		return problem;
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problem.setTitle("Unauthorized");
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The change conflicts with existing data");
		problem.setTitle("Conflict");
		return problem;
	}

	/**
	 * {@code AccessDeniedException} is normally answered by the security
	 * filter chain; this handler covers denials raised inside controllers when
	 * an error dispatch happens.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problem.setTitle("Forbidden");
		return problem;
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, ConstraintViolationException.class })
	public ProblemDetail handleValidation(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Request validation failed");
		problem.setTitle("Bad request");
		return problem;
	}

	/**
	 * Business rules that reject an otherwise well-formed request - a start
	 * time in the past, a service the doctor does not offer, a slot outside his
	 * working hours - are signalled with {@link IllegalArgumentException} by
	 * the service layer. Without this handler Spring would answer 500 and the
	 * client would get a stack trace instead of the rule it violated.
	 *
	 * <p>The exception type is caught as a whole (this is what Spring does for
	 * {@code IllegalArgumentException}); a genuine internal one would therefore
	 * also be reported as 400.</p>
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Bad request");
		return problem;
	}

}
