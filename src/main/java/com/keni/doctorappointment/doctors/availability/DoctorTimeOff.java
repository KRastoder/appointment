package com.keni.doctorappointment.doctors.availability;

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

import com.keni.doctorappointment.doctors.Doctor;

/**
 * A blocked period for a doctor (vacation, sick leave, ...).
 */
@Entity
@Table(name = "doctor_time_off")
@Getter
@Setter
@NoArgsConstructor
public class DoctorTimeOff {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private Doctor doctor;

	@Column(name = "start_at", nullable = false)
	private java.time.OffsetDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private java.time.OffsetDateTime endAt;

	@Column(name = "reason")
	private String reason;

	public DoctorTimeOff(Doctor doctor, java.time.OffsetDateTime startAt, java.time.OffsetDateTime endAt, String reason) {
		this.doctor = doctor;
		this.startAt = startAt;
		this.endAt = endAt;
		this.reason = reason;
	}

}