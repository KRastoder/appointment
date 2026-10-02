package com.keni.doctorappointment.appointments;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Appointment use cases.
 *
 * <p>Already implemented (authorisation only): a doctor may approve an
 * appointment that belongs to <em>them</em>, and a doctor/patient can list
 * their own appointments.</p>
 *
 * <p>TODO (booking logic, intentionally NOT implemented here):</p>
 * <ul>
 *   <li>booking: availability check and overlapping-appointment detection</li>
 *   <li>end time derived from the service duration / doctor availability</li>
 *   <li>further status transitions (cancel, complete, no-show) and who may
 *       trigger them</li>
 *   <li>cancellation and rescheduling rules, notifications, reminders</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;

	/**
	 * Appointments of the authenticated account: a doctor sees the appointments
	 * he is booked for, a patient the appointments he booked.
	 */
	@Transactional(readOnly = true)
	public List<AppointmentResponse> findMine(AppUserPrincipal principal) {
		List<Appointment> appointments = principal.isDoctor()
				? appointmentRepository.findByDoctor_IdOrderByStartTimeDesc(principal.id())
				: appointmentRepository.findByUser_IdOrderByStartTimeDesc(principal.id());
		return toResponses(appointments);
	}

	/**
	 * The doctor's own appointments in a given status, used by
	 * {@code /api/appointments/me/pending} and {@code .../approved}.
	 */
	@Transactional(readOnly = true)
	public List<AppointmentResponse> findMineByStatus(AppUserPrincipal principal, AppointmentStatus status) {
		if (!principal.isDoctor()) {
			throw new AccessDeniedException("Only doctors have a pending/approved appointment list");
		}
		return toResponses(
				appointmentRepository.findByDoctor_IdAndStatusOrderByStartTimeDesc(principal.id(), status));
	}

	/** Single appointment, restricted to the parties involved. */
	@Transactional(readOnly = true)
	public AppointmentResponse getVisibleById(Long id, AppUserPrincipal principal) {
		Appointment appointment = loadVisible(id, principal);
		return AppointmentResponse.from(appointment);
	}

	/**
	 * Approves a {@code SCHEDULED} appointment: the appointment moves to
	 * {@code CONFIRMED}.
	 *
	 * <p>Authorisation: only a doctor who is the doctor of this very appointment
	 * may approve it. An appointment of another doctor is reported as "not
	 * found" (404) so that ids cannot be probed.</p>
	 */
	@Transactional
	public AppointmentResponse approve(Long id, AppUserPrincipal principal) {
		if (!principal.isDoctor()) {
			throw new AccessDeniedException("Only a doctor may approve an appointment");
		}

		Appointment appointment = appointmentRepository.findByIdAndDoctor_Id(id, principal.id())
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));

		if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
			throw new AppointmentStatusConflictException("Appointment %d is %s and cannot be approved"
				.formatted(id, appointment.getStatus()));
		}

		appointment.setStatus(AppointmentStatus.CONFIRMED);
		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	private Appointment loadVisible(Long id, AppUserPrincipal principal) {
		Appointment appointment = appointmentRepository.findById(id)
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));

		boolean participant = appointment.getUser() != null && appointment.getUser().getId().equals(principal.id())
				|| appointment.getDoctor() != null && appointment.getDoctor().getId().equals(principal.id());
		if (!participant) {
			throw new AccessDeniedException("Appointment %d belongs to somebody else".formatted(id));
		}
		return appointment;
	}

	private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
		return appointments.stream().map(AppointmentResponse::from).toList();
	}

}
