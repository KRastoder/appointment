package com.keni.doctorappointment.ratings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link Rating}.
 *
 * <p>The average rating per doctor is deliberately NOT available here: it is
 * business logic to be implemented in the service layer (no cached column on
 * {@code doctors} exists).</p>
 */
public interface RatingRepository extends JpaRepository<Rating, Long> {

	List<Rating> findByDoctor_IdOrderByCreatedAtDesc(Long doctorId);

	List<Rating> findByUser_IdOrderByCreatedAtDesc(Long userId);

	Optional<Rating> findByAppointment_Id(Long appointmentId);

	@Query("""
			select r.score
			from Rating r
			where r.doctor.id = :doctorId
			""")
	List<Short> findScoresByDoctorId(Long doctorId);

	long countByDoctor_Id(Long doctorId);

}
