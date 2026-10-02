package com.keni.doctorappointment.services.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Service catalogue update body. {@code null} keeps the current value. */
public record UpdateServiceRequest(

		@Size(max = 150, message = "name must not exceed 150 characters")
		String name,

		String description,

		@Positive(message = "durationMinutes must be greater than 0")
		Integer durationMinutes,

		@DecimalMin(value = "0.00", message = "price must not be negative")
		@Digits(integer = 8, fraction = 2, message = "price must have at most 8 digits and 2 decimals")
		BigDecimal price) {

}
