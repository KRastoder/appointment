package com.keni.doctorappointment.users;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.users.dto.UserResponse;

/**
 * Placeholder endpoints for the {@code users} module (read-only).
 *
 * <p>TODO: create/update/delete endpoints, authentication, authorisation.</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public List<UserResponse> findAll() {
		return userService.findAll();
	}

	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable Long id) {
		return userService.getById(id);
	}

}
