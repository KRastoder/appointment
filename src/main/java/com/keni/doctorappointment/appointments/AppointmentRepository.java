package com.keni.doctorappointment.appointments;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Appointment}.
 *
 * <p>Deliberately free of scheduling queries: overlap/availability lookups
 * are business logic and will be added here (or via a specification) once the
 * booking rules are defined.</p>
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	List<Appointment> findByUser_IdOrderByStartTimeDesc(Long userId);

	List<Appointment> findByDoctor_IdOrderByStartTimeDesc(Long doctorId);

	List<Appointment> findByStatus(AppointmentStatus status);

}
