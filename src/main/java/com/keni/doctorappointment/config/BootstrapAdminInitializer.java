package com.keni.doctorappointment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.users.User;
import com.keni.doctorappointment.users.UserRepository;
import com.keni.doctorappointment.users.UserRole;
import com.keni.doctorappointment.users.UserService;

/**
 * Creates the very first ADMIN account on startup, because only an ADMIN may
 * create accounts - without this there would be no way in.
 *
 * <p>The account is created <b>only if the e-mail does not exist yet</b>. An
 * existing account is never touched, so changing the password afterwards
 * sticks.</p>
 *
 * <p>Configure with {@code BOOTSTRAP_ADMIN_EMAIL} and
 * {@code BOOTSTRAP_ADMIN_PASSWORD}; leave the e-mail empty to disable.</p>
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final String email;

	private final String password;

	public BootstrapAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.admin.email:}") String email,
			@Value("${app.bootstrap.admin.password:}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}

		String login = UserService.normalise(email);
		if (userRepository.existsByEmailIgnoreCase(login)) {
			log.info("Bootstrap admin {} already exists", login);
			return;
		}

		userRepository.save(new User("Bootstrap", "Admin", login, passwordEncoder.encode(password), null,
				UserRole.ADMIN));
		log.warn("Created bootstrap ADMIN account '{}' - change its password immediately", login);
	}

}
