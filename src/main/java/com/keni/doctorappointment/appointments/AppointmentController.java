package com.keni.doctorappointment.appointments;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Appointment endpoints.
 *
 * <p>All endpoints require authentication (see
 * {@link com.keni.doctorappointment.config.SecurityConfig}); the doctor-only
 * ones additionally use {@code @PreAuthorize} and verify ownership in the
 * service layer.</p>
 *
 * <pre>
 * GET  /api/appointments/me            appointments of the caller (doctor or patient)
 * GET  /api/appointments/me/pending    doctor only: his SCHEDULED appointments
 * GET  /api/appointments/me/approved   doctor only: his CONFIRMED appointments
 * GET  /api/appointments/{id}          one appointment, parties only
 * POST /api/appointments/{id}/approve  doctor only, own appointment: SCHEDULED -> CONFIRMED
 * </pre>
 *
 * <p>TODO: booking, cancellation and rescheduling endpoints.</p>
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;

	/** The caller's own appointments. */
	@GetMapping("/me")
	public List<AppointmentResponse> myAppointments(@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.findMine(principal);
	}

	/** Appointments waiting for this doctor's confirmation. */
	@GetMapping("/me/pending")
	@PreAuthorize("hasRole('DOCTOR')")
	public List<AppointmentResponse> myPendingAppointments(@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.findMineByStatus(principal, AppointmentStatus.SCHEDULED);
	}

	/** Appointments this doctor has already confirmed. */
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

	/** Approves one of the doctor's own appointments. */
	@PostMapping("/{id}/approve")
	@PreAuthorize("hasRole('DOCTOR')")
	public AppointmentResponse approve(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return appointmentService.approve(id, principal);
	}

}
