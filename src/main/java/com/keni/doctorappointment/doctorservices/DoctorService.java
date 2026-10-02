package com.keni.doctorappointment.doctorservices;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.services.Service;

/**
 * Join entity between {@link Doctor} and {@link Service} (many-to-many).
 *
 * <p>A dedicated entity is used instead of a bare {@code @ManyToMany} so the
 * relation can be extended later, e.g. with a doctor-specific
 * {@code price} / {@code duration_minutes}. Those columns are intentionally
 * NOT part of the schema yet.</p>
 *
 * <p>The identifier is shared with the foreign keys via {@link MapsId}, so
 * Hibernate reads the key columns from the associations instead of writing
 * them twice.</p>
 */
@Entity
@Table(name = "doctor_services")
@Getter
@Setter
@NoArgsConstructor
public class DoctorService {

	@EmbeddedId
	private DoctorServiceId id;

	@MapsId("doctorId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@MapsId("serviceId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private Service service;

	public DoctorService(Doctor doctor, Service service) {
		this.doctor = doctor;
		this.service = service;
		this.id = new DoctorServiceId(
				doctor != null ? doctor.getId() : null,
				service != null ? service.getId() : null);
	}

	/** Convenience constructor for new rows that were not persisted yet. */
	public DoctorService(Long doctorId, Long serviceId) {
		this.id = new DoctorServiceId(doctorId, serviceId);
	}

}
