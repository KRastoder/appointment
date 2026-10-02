package com.keni.doctorappointment.users;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.common.dto.ChangePasswordRequest;
import com.keni.doctorappointment.users.dto.RegisterUserRequest;
import com.keni.doctorappointment.users.dto.UpdateUserRequest;
import com.keni.doctorappointment.users.dto.UserResponse;

/**
 * Patient and staff account endpoints.
 *
 * <pre>
 * GET    /api/users/me            own account
 * PUT    /api/users/me/password   change own password (current password required)
 * POST   /api/users               ADMIN: create any account (patient or admin)
 * GET    /api/users               ADMIN: list accounts
 * GET    /api/users/{id}          own account, ADMIN: any account
 * PUT    /api/users/{id}          own account, ADMIN: any account (incl. role)
 * DELETE /api/users/{id}          own account or ADMIN
 * </pre>
 *
 * <p>Public patient registration lives at {@code POST /api/auth/register}.</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return userService.getMine(principal);
	}

	@PutMapping("/me")
	public UserResponse updateMe(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody UpdateUserRequest request) {
		return userService.update(principal.id(), request, principal);
	}

	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(principal, request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody RegisterUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createByAdmin(request));
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UserResponse> findAll() {
		return userService.findAll();
	}

	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		return userService.getById(id, principal);
	}

	@PutMapping("/{id}")
	public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return userService.update(id, request, principal);
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal AppUserPrincipal principal) {
		userService.delete(principal.id(), principal);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
		userService.delete(id, principal);
		return ResponseEntity.noContent().build();
	}

}
