package com.keni.doctorappointment.auth;

import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.auth.dto.AuthResponse;
import com.keni.doctorappointment.auth.dto.LoginRequest;
import com.keni.doctorappointment.auth.dto.RefreshTokenRequest;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.users.UserService;
import com.keni.doctorappointment.users.dto.RegisterUserRequest;
import com.keni.doctorappointment.users.dto.UserResponse;

/**
 * Authentication endpoints.
 *
 * <pre>
 * POST /api/auth/register   public self registration (patient account)
 * POST /api/auth/login      e-mail + password -&gt; token pair
 * POST /api/auth/refresh    refresh token    -&gt; new token pair (old one is revoked)
 * POST /api/auth/logout     revokes the refresh token
 * GET  /api/auth/me         the authenticated account
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final UserService userService;

	/** Patient self registration. Admin accounts are created by an ADMIN. */
	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
		UserResponse created = userService.registerPatient(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.email(), request.password());
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public Map<String, Object> me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return Map.of(
				"id", principal.id(),
				"email", principal.getUsername(),
				"accountType", principal.accountType().name(),
				"roles", principal.roles(),
				"doctor", principal.isDoctor());
	}

}
