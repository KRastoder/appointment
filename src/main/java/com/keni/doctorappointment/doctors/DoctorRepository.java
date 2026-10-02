package com.keni.doctorappointment.doctors;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Doctor}. */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

	Optional<Doctor> findByEmail(String email);

	boolean existsByEmail(String email);

}
