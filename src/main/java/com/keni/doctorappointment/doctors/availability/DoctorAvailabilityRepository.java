package com.keni.doctorappointment.doctors.availability;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

	@Query("select a from DoctorAvailability a where a.doctor.id = :doctorId order by a.dayOfWeek, a.startTime")
	List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekStartTime(@Param("doctorId") Long doctorId);

	@Query("select a from DoctorAvailability a where a.doctor.id = :doctorId and a.dayOfWeek = :dayOfWeek")
	List<DoctorAvailability> findByDoctorIdAndDayOfWeek(@Param("doctorId") Long doctorId, @Param("dayOfWeek") int dayOfWeek);

}