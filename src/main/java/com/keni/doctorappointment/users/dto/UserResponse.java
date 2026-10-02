package com.keni.doctorappointment.users.dto;

import java.time.OffsetDateTime;

import com.keni.doctorappointment.users.User;

/**
 * Outbound representation of a {@link User}.
 *
 * <p>{@code passwordHash} is intentionally never exposed.</p>
 */
public record UserResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phoneNumber,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getPhoneNumber(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

}
