package com.keni.doctorappointment.ratings;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.keni.doctorappointment.appointments.Appointment;
import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.users.User;

/**
 * A rating a {@link User} gives to a {@link Doctor} for an {@link Appointment}.
 *
 * <p>Database guarantees:</p>
 * <ul>
 *   <li>{@code score} must be between 1 and 5 ({@code ck_ratings_score})</li>
 *   <li>a user can rate a given appointment at most once
 *       ({@code uq_ratings_user_appointment})</li>
 *   <li>{@code appointment_id} is nullable, so at most one rating exists per
 *       appointment while ratings without an appointment are still possible</li>
 * </ul>
 *
 * <p>NOT enforced yet (business logic, see README): that the rating author is
 * the patient of that appointment, and the average rating per doctor.</p>
 */
@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	/** Optional: the appointment that produced this rating. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "appointment_id")
	private Appointment appointment;

	/** 1..5, enforced by {@code ck_ratings_score}. */
	@Column(name = "score", nullable = false)
	private short score;

	@Column(name = "comment")
	private String comment;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Rating(User user, Doctor doctor, Appointment appointment, short score, String comment) {
		this.user = user;
		this.doctor = doctor;
		this.appointment = appointment;
		this.score = score;
		this.comment = comment;
	}

}
