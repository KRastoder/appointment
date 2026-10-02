package com.keni.doctorappointment.ratings;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.ratings.dto.RatingResponse;
import com.keni.doctorappointment.ratings.dto.SubmitRatingRequest;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Rating endpoints.
 *
 * <pre>
 * GET    /api/ratings                    all ratings (public)
 * GET    /api/ratings/{id}               single rating
 * GET    /api/ratings/doctor/{doctorId}  ratings for a doctor
 * GET    /api/ratings/doctor/{doctorId}/average  average rating for a doctor
 * POST   /api/ratings                    patient: submit rating for completed appointment
 * </pre>
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

	@GetMapping("/doctor/{doctorId}")
	public List<RatingResponse> getByDoctor(@PathVariable Long doctorId) {
		return ratingService.findByDoctor(doctorId);
	}

	@GetMapping("/doctor/{doctorId}/average")
	public java.util.Map<String, Object> getAverageRating(@PathVariable Long doctorId) {
		return java.util.Map.of(
				"doctorId", doctorId,
				"averageRating", ratingService.getAverageRating(doctorId),
				"ratingCount", ratingService.getRatingCount(doctorId));
	}

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<RatingResponse> submitRating(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody SubmitRatingRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ratingService.submitRating(principal, request));
	}

}