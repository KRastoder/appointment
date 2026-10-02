package com.keni.doctorappointment.doctors.availability;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorTimeOffRepository extends JpaRepository<DoctorTimeOff, Long> {

	List<DoctorTimeOff> findByDoctor_IdOrderByStartAt(Long doctorId);

}