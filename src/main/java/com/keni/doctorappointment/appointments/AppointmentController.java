package com.keni.doctorappointment.appointments;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;
import com.keni.doctorappointment.appointments.dto.BookAppointmentRequest;
import com.keni.doctorappointment.appointments.dto.CancelAppointmentRequest;
import com.keni.doctorappointment.appointments.dto.RescheduleAppointmentRequest;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Appointment endpoints.
 *
 * <pre>
 * GET    /api/appointments/me            appointments of the caller (doctor or patient)
 * GET    /api/appointments/me/pending    doctor only: his SCHEDULED appointments
 * GET    /api/appointments/me/approved   doctor only: his CONFIRMED appointments
 * GET    /api/appointments/{id}          one appointment, parties only
 * POST   /api/appointments               patient: book new appointment
 * POST   /api/appointments/{id}/approve  doctor only, own appointment: SCHEDULED -> CONFIRMED
 * POST   /api/appointments/{id}/cancel   patient/doctor/admin: SCHEDULED/CONFIRMED -> CANCELLED/NO_SHOW
 * POST   /api/appointments/{id}/reschedule patient/doctor/admin: reschedule
 * POST   /api/appointments/{id}/complete doctor only, own: CONFIRMED -> COMPLETED
 * POST   /api/appointments/{id}/no-show  doctor/admin: CONFIRMED -> NO_SHOW
 * </pre>
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;

	@GetMapping("/me")
	public List<AppointmentResponse> myAppointments(@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.findMine(principal);
	}

	@GetMapping("/me/pending")
	@PreAuthorize("hasRole('DOCTOR')")
	public List<AppointmentResponse> myPendingAppointments(@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.findMineByStatus(principal, AppointmentStatus.SCHEDULED);
	}

	@GetMapping("/me/approved")
	@PreAuthorize("hasRole('DOCTOR')")
	public List<AppointmentResponse> myApprovedAppointments(@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.findMineByStatus(principal, AppointmentStatus.CONFIRMED);
	}

	@GetMapping("/{id}")
	public AppointmentResponse getById(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.getVisibleById(id, principal);
	}

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<AppointmentResponse> book(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody BookAppointmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(appointmentService.book(principal, request));
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasRole('DOCTOR')")
	public AppointmentResponse approve(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.approve(id, principal);
	}

	@PostMapping("/{id}/cancel")
	public AppointmentResponse cancel(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody CancelAppointmentRequest request) {
		return appointmentService.cancel(id, principal, request);
	}

	@PostMapping("/{id}/reschedule")
	public AppointmentResponse reschedule(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody RescheduleAppointmentRequest request) {
		return appointmentService.reschedule(id, principal, request);
	}

	@PostMapping("/{id}/complete")
	@PreAuthorize("hasRole('DOCTOR')")
	public AppointmentResponse complete(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.complete(id, principal);
	}

	@PostMapping("/{id}/no-show")
	@PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
	public AppointmentResponse markNoShow(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.markNoShow(id, principal);
	}

}