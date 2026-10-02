package com.keni.doctorappointment.appointments;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;

/**
 * Placeholder service for the {@code appointments} module (reads only).
 *
 * <p>TODO (business logic, intentionally NOT implemented here):</p>
 * <ul>
 *   <li>booking: availability check and overlapping-appointment detection</li>
 *   <li>end time derived from the service duration / doctor availability</li>
 *   <li>status transition rules (confirm, cancel, complete, no-show)</li>
 *   <li>cancellation and rescheduling rules, notifications, reminders</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;

	@Transactional(readOnly = true)
	public List<AppointmentResponse> findAll() {
		return appointmentRepository.findAll().stream().map(AppointmentResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public AppointmentResponse getById(Long id) {
		return appointmentRepository.findById(id)
				.map(AppointmentResponse::from)
				.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));
	}

}
