package com.keni.doctorappointment.doctors;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.keni.doctorappointment.appointments.Appointment;
import com.keni.doctorappointment.doctorservices.DoctorService;
import com.keni.doctorappointment.ratings.Rating;

/**
 * A doctor who offers one or more {@link com.keni.doctorappointment.services.Service services}.
 *
 * <p>There is deliberately NO cached {@code rating} column: the average is
 * derived from the {@code ratings} table in the service layer (not implemented yet).</p>
 *
 * <p>The many-to-many to services is modelled explicitly through the
 * {@link DoctorService} join entity, not through a bare {@code @ManyToMany}.</p>
 */
@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	/** Unique per doctor; enforced by {@code uq_doctors_email}. */
	@Column(name = "email", nullable = false, length = 320)
	private String email;

	@Column(name = "phone_number", length = 32)
	private String phoneNumber;

	@Column(name = "specialization", nullable = false, length = 150)
	private String specialization;

	@Column(name = "bio")
	private String bio;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// --- relationships -------------------------------------------------

	/** Offered services; owning side is {@link DoctorService}. */
	@OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
	private Set<DoctorService> doctorServices = new LinkedHashSet<>();

	@OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
	private Set<Appointment> appointments = new LinkedHashSet<>();

	@OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
	private Set<Rating> ratings = new LinkedHashSet<>();

	public Doctor(String firstName, String lastName, String email, String phoneNumber, String specialization,
			String bio) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.specialization = specialization;
		this.bio = bio;
	}

}
