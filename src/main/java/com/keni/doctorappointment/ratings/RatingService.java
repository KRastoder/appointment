package com.keni.doctorappointment.ratings;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.ratings.dto.RatingResponse;

/**
 * Placeholder service for the {@code ratings} module (reads only).
 *
 * <p>TODO (business logic, intentionally NOT implemented here):</p>
 * <ul>
 *   <li>validation that only the patient of that appointment may rate a doctor</li>
 *   <li>score range validation (the database already rejects values outside 1..5)</li>
 *   <li>one rating per appointment per user</li>
 *   <li>average rating per doctor (computed from {@code ratings}, not cached)</li>
 *   <li>rating edits / moderation</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RatingService {

	private final RatingRepository ratingRepository;

	@Transactional(readOnly = true)
	public List<RatingResponse> findAll() {
		return ratingRepository.findAll().stream().map(RatingResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public RatingResponse getById(Long id) {
		return ratingRepository.findById(id)
				.map(RatingResponse::from)
				.orElseThrow(() -> new NoSuchElementException("Rating %d not found".formatted(id)));
	}

}
