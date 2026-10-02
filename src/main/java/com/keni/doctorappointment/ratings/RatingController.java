package com.keni.doctorappointment.ratings;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.ratings.dto.RatingResponse;

/**
 * Placeholder endpoints for the {@code ratings} module (read-only).
 *
 * <p>TODO: "rate this doctor" endpoint plus rating validation and the
 * average rating per doctor.</p>
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

	private final RatingService ratingService;

	@GetMapping
	public List<RatingResponse> findAll() {
		return ratingService.findAll();
	}

	@GetMapping("/{id}")
	public RatingResponse getById(@PathVariable Long id) {
		return ratingService.getById(id);
	}

}
