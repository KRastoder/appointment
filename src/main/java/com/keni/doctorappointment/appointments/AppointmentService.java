package com.keni.doctorappointment.appointments;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.appointments.dto.AppointmentResponse;
import com.keni.doctorappointment.appointments.dto.BookAppointmentRequest;
import com.keni.doctorappointment.appointments.dto.CancelAppointmentRequest;
import com.keni.doctorappointment.appointments.dto.RescheduleAppointmentRequest;
import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.doctors.availability.DoctorAvailabilityService;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.users.User;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Appointment use cases.
 *
 * <p>Implements booking, cancellation, rescheduling, and status transitions.</p>
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;
	private final UserRepository userRepository;
	private final DoctorRepository doctorRepository;
	private final ServiceRepository serviceRepository;
	private final DoctorAvailabilityService availabilityService;

	/**
	 * All "now" comparisons go through this clock (see {@code TimeConfig}), so a
	 * test can move time forward instead of waiting for a real appointment to
	 * end.
	 */
	private final Clock clock;

	// --- read ------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<AppointmentResponse> findMine(AppUserPrincipal principal) {
		List<Appointment> appointments = principal.isDoctor()
				? appointmentRepository.findByDoctor_IdOrderByStartTimeDesc(principal.id())
				: appointmentRepository.findByUser_IdOrderByStartTimeDesc(principal.id());
		return toResponses(appointments);
	}

	@Transactional(readOnly = true)
	public List<AppointmentResponse> findMineByStatus(AppUserPrincipal principal, AppointmentStatus status) {
		if (!principal.isDoctor()) {
			throw new AccessDeniedException("Only doctors have a pending/approved appointment list");
		}
		return toResponses(
				appointmentRepository.findByDoctor_IdAndStatusOrderByStartTimeDesc(principal.id(), status));
	}

	@Transactional(readOnly = true)
	public AppointmentResponse getVisibleById(Long id, AppUserPrincipal principal) {
		Appointment appointment = loadVisible(id, principal);
		return AppointmentResponse.from(appointment);
	}

	// --- book ------------------------------------------------------------

	/**
	 * Books a new appointment.
	 *
	 * <p>Checks:</p>
	 * <ul>
	 *   <li>Patient must exist and be the caller</li>
	 *   <li>Doctor must exist</li>
	 *   <li>Service must exist and be offered by the doctor</li>
	 *   <li>Start time must be in the future</li>
	 *   <li>End time is derived from service duration</li>
	 *   <li>Doctor must be available (weekly schedule, no time-off)</li>
	 *   <li>No overlapping appointment for the same doctor (enforced by DB exclusion constraint)</li>
	 * </ul>
	 */
	@Transactional
	public AppointmentResponse book(AppUserPrincipal principal, BookAppointmentRequest request) {
		if (!principal.isPatient()) {
			throw new AccessDeniedException("Only patients may book appointments");
		}

		// Validate entities
		User patient = userRepository.getReferenceById(principal.id());
		com.keni.doctorappointment.services.Service service = serviceRepository.findById(request.serviceId())
			.orElseThrow(() -> new NoSuchElementException("Service %d not found".formatted(request.serviceId())));

		com.keni.doctorappointment.doctors.Doctor doctor = doctorRepository.findById(request.doctorId())
			.orElseThrow(() -> new NoSuchElementException("Doctor %d not found".formatted(request.doctorId())));

		// Check doctor offers this service
		if (!doctor.getDoctorServices().stream()
			.anyMatch(ds -> ds.getService().getId().equals(service.getId()))) {
			throw new IllegalArgumentException("Doctor does not offer the requested service");
		}

		OffsetDateTime startTime = request.startTime();
		if (startTime.isBefore(OffsetDateTime.now(clock))) {
			throw new IllegalArgumentException("Start time must be in the future");
		}

		// Derive end time from service duration
		OffsetDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

		// Availability check
		if (!availabilityService.isAvailable(doctor, startTime, endTime)) {
			throw new IllegalArgumentException("Doctor is not available at the requested time");
		}

		// Create appointment
		Appointment appointment = new Appointment(
				patient, doctor, service, startTime, endTime, AppointmentStatus.SCHEDULED, request.notes());

		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	// --- approve (doctor) ------------------------------------------------

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

	// --- cancel ----------------------------------------------------------

	/**
	 * Cancels an appointment.
	 *
	 * <p>Rules:</p>
	 * <ul>
	 *   <li>Patient may cancel their own appointment</li>
	 *   <li>Doctor may cancel their own appointment</li>
	 *   <li>Admin may cancel any appointment</li>
	 *   <li>Only SCHEDULED or CONFIRMED appointments may be cancelled</li>
	 *   <li>If cancelled less than 24h before start, marked as NO_SHOW for patient (business rule)</li>
	 * </ul>
	 */
	@Transactional
	public AppointmentResponse cancel(Long id, AppUserPrincipal principal, CancelAppointmentRequest request) {
		Appointment appointment = loadVisible(id, principal);

		if (appointment.getStatus() != AppointmentStatus.SCHEDULED
				&& appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new AppointmentStatusConflictException("Appointment %d is %s and cannot be cancelled"
				.formatted(id, appointment.getStatus()));
		}

		// Check if cancelled less than 24h before
		OffsetDateTime now = OffsetDateTime.now(clock);
		boolean lateCancellation = now.isAfter(appointment.getStartTime().minusHours(24));

		if (lateCancellation && !principal.isAdmin()) {
			// For patients cancelling late, mark as NO_SHOW (penalty)
			if (principal.isPatient()) {
				appointment.setStatus(AppointmentStatus.NO_SHOW);
			} else {
				appointment.setStatus(AppointmentStatus.CANCELLED);
			}
		} else {
			appointment.setStatus(AppointmentStatus.CANCELLED);
		}

		if (request.reason() != null) {
			appointment.setNotes((appointment.getNotes() != null ? appointment.getNotes() + "; " : "")
				+ "Cancelled: " + request.reason());
		}

		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	// --- reschedule ------------------------------------------------------

	/**
	 * Reschedules an appointment to a new time.
	 *
	 * <p>Rules:</p>
	 * <ul>
	 *   <li>Patient may reschedule their own appointment</li>
	 *   <li>Doctor may reschedule their own appointment</li>
	 *   <li>Admin may reschedule any appointment</li>
	 *   <li>Only SCHEDULED or CONFIRMED appointments may be rescheduled</li>
	 *   <li>New time must pass all booking checks (availability, no overlap)</li>
	 * </ul>
	 */
	@Transactional
	public AppointmentResponse reschedule(Long id, AppUserPrincipal principal, RescheduleAppointmentRequest request) {
		Appointment appointment = loadVisible(id, principal);

		if (appointment.getStatus() != AppointmentStatus.SCHEDULED
				&& appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new AppointmentStatusConflictException("Appointment %d is %s and cannot be rescheduled"
				.formatted(id, appointment.getStatus()));
		}

		OffsetDateTime newStartTime = request.newStartTime();
		if (newStartTime.isBefore(OffsetDateTime.now(clock))) {
			throw new IllegalArgumentException("New start time must be in the future");
		}

		// End time derived from service duration
		OffsetDateTime newEndTime = newStartTime.plusMinutes(appointment.getService().getDurationMinutes());

		// Availability check for new time
		if (!availabilityService.isAvailable(appointment.getDoctor(), newStartTime, newEndTime)) {
			throw new IllegalArgumentException("Doctor is not available at the requested time");
		}

		appointment.setStartTime(newStartTime);
		appointment.setEndTime(newEndTime);
		appointment.setStatus(AppointmentStatus.SCHEDULED); // resetting to scheduled for re-confirmation

		if (request.reason() != null) {
			appointment.setNotes((appointment.getNotes() != null ? appointment.getNotes() + "; " : "")
				+ "Rescheduled: " + request.reason());
		}

		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	// --- complete (doctor) -----------------------------------------------

	/**
	 * Marks an appointment as completed.
	 *
	 * <p>Only the doctor who owns the appointment may complete it.
	 * Only CONFIRMED appointments may be completed.</p>
	 */
	@Transactional
	public AppointmentResponse complete(Long id, AppUserPrincipal principal) {
		if (!principal.isDoctor()) {
			throw new AccessDeniedException("Only a doctor may complete an appointment");
		}

		Appointment appointment = appointmentRepository.findByIdAndDoctor_Id(id, principal.id())
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));

		if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new AppointmentStatusConflictException("Appointment %d is %s and cannot be completed"
				.formatted(id, appointment.getStatus()));
		}

		if (appointment.getEndTime().isAfter(OffsetDateTime.now(clock))) {
			throw new AppointmentStatusConflictException("Appointment has not ended yet");
		}

		appointment.setStatus(AppointmentStatus.COMPLETED);
		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	// --- no-show (doctor or auto) ----------------------------------------

	/**
	 * Marks an appointment as NO_SHOW.
	 *
	 * <p>Doctor may mark an appointment as NO_SHOW if the patient didn't show up.
	 * Only CONFIRMED appointments past their end time may be marked NO_SHOW.</p>
	 */
	@Transactional
	public AppointmentResponse markNoShow(Long id, AppUserPrincipal principal) {
		if (!principal.isDoctor() && !principal.isAdmin()) {
			throw new AccessDeniedException("Only a doctor or admin may mark no-show");
		}

		Appointment appointment = appointmentRepository.findByIdAndDoctor_Id(id, principal.id())
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));

		if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new AppointmentStatusConflictException("Appointment %d is %s and cannot be marked as no-show"
				.formatted(id, appointment.getStatus()));
		}

		if (appointment.getEndTime().isAfter(OffsetDateTime.now(clock))) {
			throw new AppointmentStatusConflictException("Appointment has not ended yet");
		}

		appointment.setStatus(AppointmentStatus.NO_SHOW);
		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	// --- helpers ---------------------------------------------------------

	private Appointment loadVisible(Long id, AppUserPrincipal principal) {
		Appointment appointment = appointmentRepository.findById(id)
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(id)));

		boolean participant = principal.isDoctor()
				? appointment.getDoctor() != null && appointment.getDoctor().getId().equals(principal.id())
				: appointment.getUser() != null && appointment.getUser().getId().equals(principal.id());
		if (!participant && !principal.isAdmin()) {
			throw new AccessDeniedException("Appointment %d belongs to somebody else".formatted(id));
		}
		return appointment;
	}

	private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
		return appointments.stream().map(AppointmentResponse::from).toList();
	}

}