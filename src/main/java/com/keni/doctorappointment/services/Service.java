package com.keni.doctorappointment.services;

import java.math.BigDecimal;
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

/**
 * A bookable service in the catalogue (e.g. "Dental Examination").
 *
 * <p>A service can be offered by several doctors and a doctor can offer
 * several services. The many-to-many is expressed explicitly with the
 * {@link DoctorService} join entity so that per-doctor overrides
 * (price, duration, ...) can be added later without restructuring.</p>
 *
 * <p>Note: this class is called {@code Service} to match the domain and the
 * table name. That collides with Spring's {@code @Service} stereotype, so in
 * this package the stereotype is referenced with its fully qualified name
 * (see {@code ServiceService} / {@code ServiceController}).</p>
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class Service {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Unique; enforced by {@code uq_services_name}. */
	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description")
	private String description;

	/** Default duration in minutes; enforced by {@code ck_services_duration_minutes}. */
	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;

	/** Default price; enforced by {@code ck_services_price}. */
	@Column(name = "price", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// --- relationships -------------------------------------------------

	/** Doctors offering this service; owning side is {@link DoctorService}. */
	@OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
	private Set<DoctorService> doctorServices = new LinkedHashSet<>();

	@OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
	private Set<Appointment> appointments = new LinkedHashSet<>();

	public Service(String name, String description, Integer durationMinutes, BigDecimal price) {
		this.name = name;
		this.description = description;
		this.durationMinutes = durationMinutes;
		this.price = price;
	}

}
