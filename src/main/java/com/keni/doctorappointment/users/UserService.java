package com.keni.doctorappointment.users;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.users.dto.UserResponse;

/**
 * Placeholder service for the {@code users} module.
 *
 * <p>Currently only read operations used by the placeholder endpoints.</p>
 *
 * <p>TODO (business logic, intentionally not implemented here):</p>
 * <ul>
 *   <li>registration / e-mail uniqueness handling</li>
 *   <li>profile updates</li>
 *   <li>authentication and authorisation</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userRepository.findAll().stream().map(UserResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public UserResponse getById(Long id) {
		return userRepository.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> new NoSuchElementException("User %d not found".formatted(id)));
	}

}
