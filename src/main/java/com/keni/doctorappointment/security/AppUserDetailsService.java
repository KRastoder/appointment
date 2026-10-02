package com.keni.doctorappointment.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.users.UserRepository;
import com.keni.doctorappointment.users.UserRole;

/**
 * Resolves credentials by e-mail for all three account types.
 *
 * <p>Passwords are never compared here: {@code AuthService} verifies the BCrypt
 * hash with the configured {@code PasswordEncoder}.</p>
 *
 * <p>Lookup is case insensitive (e-mails are stored lower case and indexed on
 * {@code LOWER(email)}). A doctor record wins over a patient record for the
 * same address; registration prevents that situation in the first place.</p>
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final DoctorRepository doctorRepository;

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		String login = email != null ? email.trim() : "";

		return doctorRepository.findByEmailIgnoreCase(login)
				.map(doctor -> AppUserPrincipal.ofDoctor(doctor.getId(), doctor.getEmail(),
						doctor.getPasswordHash()))
				.or(() -> userRepository.findByEmailIgnoreCase(login)
					.map(user -> user.getRole() == UserRole.ADMIN
							? AppUserPrincipal.ofAdmin(user.getId(), user.getEmail(), user.getPasswordHash())
							: AppUserPrincipal.ofPatient(user.getId(), user.getEmail(), user.getPasswordHash())))
				.orElseThrow(() -> new UsernameNotFoundException("No account for e-mail " + login));
	}

}
