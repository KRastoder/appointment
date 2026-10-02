package com.keni.doctorappointment.users;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.common.ResourceConflictException;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.security.DeletedAccountRegistry;
import com.keni.doctorappointment.common.dto.ChangePasswordRequest;
import com.keni.doctorappointment.users.dto.RegisterUserRequest;
import com.keni.doctorappointment.users.dto.UpdateUserRequest;
import com.keni.doctorappointment.users.dto.UserResponse;

/**
 * Patient and staff (ADMIN) account management.
 *
 * <p>Password rules are enforced by the request DTOs; hashing and e-mail
 * normalisation/uniqueness happen here. E-mails are stored lower case and the
 * database index on {@code LOWER(email)} makes them unique case insensitively.
 * Because patients and doctors live in two tables, cross-table uniqueness is
 * enforced here - this is what stops "one address, two accounts" and the
 * resulting ambiguity at login time.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	private final DoctorRepository doctorRepository;

	private final PasswordEncoder passwordEncoder;

	private final DeletedAccountRegistry deletedAccounts;

	/** Public self registration: always a patient account. */
	@Transactional
	public UserResponse registerPatient(RegisterUserRequest request) {
		return create(request, UserRole.PATIENT);
	}

	/** Account creation by an ADMIN; the requested role is honoured. */
	@Transactional
	public UserResponse createByAdmin(RegisterUserRequest request) {
		return create(request, request.role() != null ? request.role() : UserRole.PATIENT);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userRepository.findAll().stream().map(UserResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public UserResponse getMine(AppUserPrincipal principal) {
		return UserResponse.from(requireOwnedUser(principal));
	}

	/** Reads one account: own account, or any account when the caller is an ADMIN. */
	@Transactional(readOnly = true)
	public UserResponse getById(Long id, AppUserPrincipal principal) {
		assertCanManage(principal, id);
		return UserResponse.from(requireUser(id));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request, AppUserPrincipal principal) {
		assertCanManage(principal, id);
		User user = requireUser(id);

		if (request.firstName() != null) {
			user.setFirstName(request.firstName());
		}
		if (request.lastName() != null) {
			user.setLastName(request.lastName());
		}
		if (request.phoneNumber() != null) {
			user.setPhoneNumber(request.phoneNumber());
		}
		if (request.email() != null) {
			String email = normalise(request.email());
			assertEmailAvailable(email, user.getId());
			user.setEmail(email);
		}
		if (request.role() != null) {
			// Only an ADMIN may change roles, never the account holder.
			if (!principal.isAdmin()) {
				throw new AccessDeniedException("Only an ADMIN may change roles");
			}
			user.setRole(request.role());
		}

		return UserResponse.from(userRepository.save(user));
	}

	@Transactional
	public void delete(Long id, AppUserPrincipal principal) {
		if (!isSelf(id, principal) && !principal.isAdmin()) {
			throw new AccessDeniedException("Only an ADMIN may delete other accounts");
		}
		User user = requireUser(id);
		assertNoBookings(user);
		userRepository.delete(user);
		// The account is gone, so its outstanding access tokens must stop working.
		deletedAccounts.invalidate(user.getRole() == UserRole.ADMIN
				? AppUserPrincipal.AccountType.ADMIN
				: AppUserPrincipal.AccountType.PATIENT, user.getId());
	}

	/** Password change for the authenticated account. */
	@Transactional
	public void changePassword(AppUserPrincipal principal, ChangePasswordRequest request) {
		User user = requireOwnedUser(principal);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadCredentialsException("Current password is wrong");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	// --- helpers -------------------------------------------------------

	private UserResponse create(RegisterUserRequest request, UserRole role) {
		String email = normalise(request.email());
		assertEmailAvailable(email, null);

		User user = new User(request.firstName(), request.lastName(), email,
				passwordEncoder.encode(request.password()), request.phoneNumber(), role);
		return UserResponse.from(userRepository.save(user));
	}

	/**
	 * Ensures the address is unused in <em>both</em> account tables. A unique
	 * index cannot span two tables, so this check lives in the service layer.
	 */
	void assertEmailAvailable(String email, Long ownUserId) {
		userRepository.findByEmailIgnoreCase(email)
			.filter(existing -> ownUserId == null || !existing.getId().equals(ownUserId))
			.ifPresent(existing -> {
				throw new EmailAlreadyInUseException(email);
			});

		if (doctorRepository.existsByEmailIgnoreCase(email)) {
			throw new EmailAlreadyInUseException(email);
		}
	}

	private User requireUser(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new NoSuchElementException("User %d not found".formatted(id)));
	}

	private User requireOwnedUser(AppUserPrincipal principal) {
		if (principal.isDoctor()) {
			throw new AccessDeniedException("This endpoint is for patient accounts");
		}
		return requireUser(principal.id());
	}

	private void assertCanManage(AppUserPrincipal principal, Long targetId) {
		if (!isSelf(targetId, principal) && !principal.isAdmin()) {
			throw new AccessDeniedException("Only an ADMIN may manage other accounts");
		}
	}

	private boolean isSelf(Long targetId, AppUserPrincipal principal) {
		return !principal.isDoctor() && principal.id().equals(targetId);
	}

	/** Appointments must not be orphaned; deleting a patient with bookings fails. */
	private void assertNoBookings(User user) {
		if (!user.getAppointments().isEmpty()) {
			throw new ResourceConflictException(
					"User %d still has appointments and cannot be deleted".formatted(user.getId()));
		}
	}

	public static String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
