package com.keni.doctorappointment.services.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Service catalogue creation body (ADMIN only). */
public record CreateServiceRequest(

		@NotBlank(message = "name is required")
		@Size(max = 150, message = "name must not exceed 150 characters")
		String name,

		String description,

		@NotNull(message = "durationMinutes is required")
		@Positive(message = "durationMinutes must be greater than 0")
		Integer durationMinutes,

		@NotNull(message = "price is required")
		@DecimalMin(value = "0.00", message = "price must not be negative")
		@Digits(integer = 8, fraction = 2, message = "price must have at most 8 digits and 2 decimals")
		BigDecimal price) {

}
