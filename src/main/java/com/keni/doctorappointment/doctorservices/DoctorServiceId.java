package com.keni.doctorappointment.doctorservices;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite key of the {@code doctor_services} table
 * ({@code PRIMARY KEY (doctor_id, service_id)}).
 *
 * <p>The key is {@code Serializable} (JPA requirement for an {@code @EmbeddedId})
 * and implements {@code equals}/{@code hashCode} on both columns.</p>
 */
@Embeddable
public class DoctorServiceId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "doctor_id", nullable = false)
	private Long doctorId;

	@Column(name = "service_id", nullable = false)
	private Long serviceId;

	protected DoctorServiceId() {
		// for JPA
	}

	public DoctorServiceId(Long doctorId, Long serviceId) {
		this.doctorId = doctorId;
		this.serviceId = serviceId;
	}

	public Long getDoctorId() {
		return this.doctorId;
	}

	public void setDoctorId(Long doctorId) {
		this.doctorId = doctorId;
	}

	public Long getServiceId() {
		return this.serviceId;
	}

	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof DoctorServiceId that)) {
			return false;
		}
		return Objects.equals(this.doctorId, that.doctorId)
				&& Objects.equals(this.serviceId, that.serviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.doctorId, this.serviceId);
	}

	@Override
	public String toString() {
		return "DoctorServiceId[doctorId=%s, serviceId=%s]".formatted(this.doctorId, this.serviceId);
	}

}
