package com.keni.doctorappointment.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User}. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

}
