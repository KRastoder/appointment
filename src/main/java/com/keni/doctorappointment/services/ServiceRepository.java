package com.keni.doctorappointment.services;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the {@link Service} catalogue. */
public interface ServiceRepository extends JpaRepository<Service, Long> {

	Optional<Service> findByName(String name);

	boolean existsByName(String name);

}
