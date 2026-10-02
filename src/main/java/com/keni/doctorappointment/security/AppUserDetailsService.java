package com.keni.doctorappointment.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Resolves credentials by e-mail for both account types: doctors first
 * ({@code ROLE_DOCTOR}), then patients ({@code ROLE_USER}).
 *
 * <p>Passwords are never compared here: the {@code DaoAuthenticationProvider}
 * configured in {@link com.keni.doctorappointment.config.SecurityConfig}
 * verifies the BCrypt hash.</p>
 *
 * <p>TODO: e-mail normalisation (case folding) and "doctor and patient share
 * the same address" handling once the account rules are defined.</p>
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

		return doctorRepository.findByEmail(login)
				.map(doctor -> AppUserPrincipal.ofDoctor(doctor.getId(), doctor.getEmail(),
						doctor.getPasswordHash()))
				.or(() -> userRepository.findByEmail(login)
					.map(user -> AppUserPrincipal.ofPatient(user.getId(), user.getEmail(), user.getPasswordHash())))
				.orElseThrow(() -> new UsernameNotFoundException("No account for e-mail " + login));
	}

}
