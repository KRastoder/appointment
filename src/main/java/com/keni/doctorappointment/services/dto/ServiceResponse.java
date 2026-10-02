package com.keni.doctorappointment.services.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.keni.doctorappointment.services.Service;

/** Outbound representation of a catalogue {@link Service}. */
public record ServiceResponse(
		Long id,
		String name,
		String description,
		Integer durationMinutes,
		BigDecimal price,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static ServiceResponse from(Service service) {
		return new ServiceResponse(
				service.getId(),
				service.getName(),
				service.getDescription(),
				service.getDurationMinutes(),
				service.getPrice(),
				service.getCreatedAt(),
				service.getUpdatedAt());
	}

}
