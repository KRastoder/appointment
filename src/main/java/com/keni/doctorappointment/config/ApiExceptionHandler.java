package com.keni.doctorappointment.config;

import java.util.NoSuchElementException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.keni.doctorappointment.appointments.AppointmentStatusConflictException;

/**
 * Minimal, generic error mapping for the placeholder endpoints.
 *
 * <p>Only technical failures are translated here (missing resource, bean
 * validation, illegal status change). Domain errors such as "slot already
 * booked" are business logic and should be added as dedicated exceptions
 * once those rules exist.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NoSuchElementException.class)
	public ProblemDetail handleNotFound(NoSuchElementException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Not found");
		return problem;
	}

	@ExceptionHandler(AppointmentStatusConflictException.class)
	public ProblemDetail handleConflict(AppointmentStatusConflictException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflict");
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
