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
import com.keni.doctorappointment.users.EmailAlreadyInUseException;

/**
 * Error mapping shared by all modules.
 *
 * <p>Technical failures and the authorisation rules are translated here
 * (404 / 409 / 400 / 401). Domain errors such as "slot already booked" are
 * business logic and should be added as dedicated exceptions once those rules
 * exist.</p>
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
			IllegalStateException.class })
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

}
