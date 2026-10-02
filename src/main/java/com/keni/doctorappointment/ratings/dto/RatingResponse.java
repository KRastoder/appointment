package com.keni.doctorappointment.ratings.dto;

import java.time.OffsetDateTime;

import com.keni.doctorappointment.ratings.Rating;

/** Outbound representation of a {@link Rating}. */
public record RatingResponse(
		Long id,
		Long userId,
		Long doctorId,
		Long appointmentId,
		short score,
		String comment,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static RatingResponse from(Rating rating) {
		return new RatingResponse(
				rating.getId(),
				rating.getUser() != null ? rating.getUser().getId() : null,
				rating.getDoctor() != null ? rating.getDoctor().getId() : null,
				rating.getAppointment() != null ? rating.getAppointment().getId() : null,
				rating.getScore(),
				rating.getComment(),
				rating.getCreatedAt(),
				rating.getUpdatedAt());
	}

}
