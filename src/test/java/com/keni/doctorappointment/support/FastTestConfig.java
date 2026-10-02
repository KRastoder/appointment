package com.keni.doctorappointment.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only beans.
 *
 * <p>The default delegating encoder uses BCrypt cost 10 (~100 ms per hash).
 * Every register/login in the HTTP tests performs several hashes, so the
 * suite spends tens of seconds on cryptography. Cost 4 keeps the same code
 * paths but is ~25x faster - more than safe enough for a throw-away test
 * database.</p>
 */
@TestConfiguration
public class FastTestConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(4);
	}

}
