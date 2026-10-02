package com.keni.doctorappointment.appointments;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.services.Service;
import com.keni.doctorappointment.users.User;

/**
 * A booked slot between a {@link User} (patient), a {@link Doctor} and a
 * {@link Service}.
 *
 * <p>Timestamps are timezone-aware ({@code TIMESTAMP WITH TIME ZONE} in
 * PostgreSQL, {@link OffsetDateTime} in Java) so that a clinic in a
 * non-UTC zone can be represented without ambiguity.</p>
 *
 * <p>Nothing about <em>when</em> an appointment may be created is enforced
 * here on purpose: conflict detection, availability and status transitions
 * are business logic and belong in the service layer (not implemented yet).</p>
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private Service service;

	@Column(name = "start_time", nullable = false)
	private OffsetDateTime startTime;

	@Column(name = "end_time", nullable = false)
	private OffsetDateTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AppointmentStatus status = AppointmentStatus.SCHEDULED;

	/** Free-text note from the patient; optional. */
	@Column(name = "notes")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Appointment(User user, Doctor doctor, Service service, OffsetDateTime startTime, OffsetDateTime endTime,
			AppointmentStatus status, String notes) {
		this.user = user;
		this.doctor = doctor;
		this.service = service;
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = status != null ? status : AppointmentStatus.SCHEDULED;
		this.notes = notes;
	}

}
