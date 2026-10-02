package com.keni.doctorappointment.ratings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.appointments.Appointment;
import com.keni.doctorappointment.appointments.AppointmentRepository;
import com.keni.doctorappointment.appointments.AppointmentStatus;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.ratings.dto.RatingResponse;
import com.keni.doctorappointment.ratings.dto.SubmitRatingRequest;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Rating use cases.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Only the patient who had the appointment may rate the doctor</li>
 *   <li>Appointment must be COMPLETED</li>
 *   <li>One rating per appointment (enforced by DB unique constraint)</li>
 *   <li>Score must be 1-5</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RatingService {

	private final RatingRepository ratingRepository;
	private final AppointmentRepository appointmentRepository;
	private final DoctorRepository doctorRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<RatingResponse> findAll() {
		return ratingRepository.findAll().stream().map(RatingResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public RatingResponse getById(Long id) {
		return ratingRepository.findById(id)
				.map(RatingResponse::from)
				.orElseThrow(() -> new NoSuchElementException("Rating %d not found".formatted(id)));
	}

	@Transactional(readOnly = true)
	public List<RatingResponse> findByDoctor(Long doctorId) {
		return ratingRepository.findByDoctor_IdOrderByCreatedAtDesc(doctorId)
			.stream().map(RatingResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public BigDecimal getAverageRating(Long doctorId) {
		List<Short> scores = ratingRepository.findScoresByDoctorId(doctorId);
		if (scores.isEmpty()) {
			return BigDecimal.ZERO;
		}
		int sum = scores.stream().mapToInt(Short::intValue).sum();
		return BigDecimal.valueOf(sum)
			.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
	}

	@Transactional(readOnly = true)
	public long getRatingCount(Long doctorId) {
		return ratingRepository.countByDoctor_Id(doctorId);
	}

	/**
	 * Submits a rating for a doctor.
	 *
	 * <p>Checks:</p>
 *   <li>Appointment exists and is COMPLETED</li>
 *   <li>Caller is the patient of that appointment</li>
 *   <li>Score is 1-5</li>
 *   <li>No existing rating for this appointment</li>
 * </ul>
	 */
	@Transactional
	public RatingResponse submitRating(AppUserPrincipal principal, SubmitRatingRequest request) {
		if (!principal.isPatient()) {
			throw new AccessDeniedException("Only patients may submit ratings");
		}

		Appointment appointment = appointmentRepository.findById(request.appointmentId())
			.orElseThrow(() -> new NoSuchElementException("Appointment %d not found".formatted(request.appointmentId())));

		// Verify patient owns this appointment
		if (!appointment.getUser().getId().equals(principal.id())) {
			throw new AccessDeniedException("You can only rate your own appointments");
		}

		// Appointment must be completed
		if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
			throw new IllegalStateException("Can only rate completed appointments");
		}

		// Check rating already exists
		if (ratingRepository.findByAppointment_Id(request.appointmentId()).isPresent()) {
			throw new IllegalStateException("Rating for this appointment already exists");
		}

		// Validate score
		if (request.score() < 1 || request.score() > 5) {
			throw new IllegalArgumentException("Score must be between 1 and 5");
		}

		Rating rating = new Rating(
				userRepository.getReferenceById(principal.id()),
				appointment.getDoctor(),
				appointment,
				(short) request.score(),
				request.comment());

		return RatingResponse.from(ratingRepository.save(rating));
	}

}