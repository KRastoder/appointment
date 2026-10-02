package com.keni.doctorappointment.ratings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request to submit a rating. */
public record SubmitRatingRequest(

		@NotNull(message = "appointmentId is required")
		Long appointmentId,

		@NotNull(message = "score is required")
		@Min(value = 1, message = "Score must be at least 1")
		@Max(value = 5, message = "Score must be at most 5")
		Integer score,

		String comment) {

}