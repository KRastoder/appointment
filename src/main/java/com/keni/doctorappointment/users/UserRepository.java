package com.keni.doctorappointment.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link User}.
 *
 * <p>Only plain CRUD + lookup helpers are provided; query methods that
 * encode business rules (e.g. duplicate-email handling) belong to the
 * service layer.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

}
