package com.keni.doctorappointment.appointments;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;

/**
 * Placeholder endpoints for the {@code appointments} module (read-only).
 *
 * <p>TODO: booking, cancellation, rescheduling and status change endpoints.</p>
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;

	@GetMapping
	public List<AppointmentResponse> findAll() {
		return appointmentService.findAll();
	}

	@GetMapping("/{id}")
	public AppointmentResponse getById(@PathVariable Long id) {
		return appointmentService.getById(id);
	}

}
