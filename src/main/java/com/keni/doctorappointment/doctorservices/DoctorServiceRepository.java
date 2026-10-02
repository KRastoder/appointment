package com.keni.doctorappointment.doctorservices;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the doctor/service join table.
 *
 * <p>The id type is the composite key {@link DoctorServiceId}, which already
 * guarantees uniqueness of a (doctor, service) pair.</p>
 */
public interface DoctorServiceRepository extends JpaRepository<DoctorService, DoctorServiceId> {

	List<DoctorService> findByDoctor_IdOrderByService_Id(Long doctorId);

	List<DoctorService> findByService_IdOrderByDoctor_Id(Long serviceId);

}
