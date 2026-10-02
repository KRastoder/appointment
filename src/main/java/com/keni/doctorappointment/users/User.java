package com.keni.doctorappointment.users;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import com.keni.doctorappointment.ratings.Rating;

/**
 * A patient / customer who can book appointments.
 *
 * <p>Mapped to the {@code users} table (the table name is required because
 * {@code user} is a reserved word in PostgreSQL).</p>
 *
 * <p>{@code passwordHash} holds an already-hashed password. Authentication
 * is deliberately NOT implemented yet.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	/** Unique per user, case insensitive; enforced by {@code uq_users_email_lower}. */
	@Column(name = "email", nullable = false, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "phone_number", length = 32)
	private String phoneNumber;

	/** PATIENT or ADMIN; enforced by {@code ck_users_role}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role = UserRole.PATIENT;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// --- relationships -------------------------------------------------
	// Owner of the association is Appointment / Rating (the @ManyToOne side).

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private Set<Appointment> appointments = new LinkedHashSet<>();

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private Set<Rating> ratings = new LinkedHashSet<>();

	public User(String firstName, String lastName, String email, String passwordHash, String phoneNumber) {
		this(firstName, lastName, email, passwordHash, phoneNumber, UserRole.PATIENT);
	}

	public User(String firstName, String lastName, String email, String passwordHash, String phoneNumber,
			UserRole role) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.phoneNumber = phoneNumber;
		this.role = role != null ? role : UserRole.PATIENT;
	}

}
