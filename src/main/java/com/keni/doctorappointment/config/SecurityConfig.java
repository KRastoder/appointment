package com.keni.doctorappointment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.keni.doctorappointment.security.AppUserDetailsService;

/**
 * Security configuration of the monolith.
 *
 * <p>Chosen setup (deliberately minimal, no extra infrastructure):</p>
 * <ul>
 *   <li>stateless HTTP Basic authentication (no session, no CSRF token needed)</li>
 *   <li>BCrypt password hashing via {@link PasswordEncoder}</li>
 *   <li>two roles: {@code ROLE_DOCTOR} and {@code ROLE_USER}</li>
 *   <li>{@code @EnableMethodSecurity} so ownership rules can be expressed with
 *       {@code @PreAuthorize} inside the feature modules</li>
 * </ul>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>the catalogue (services, doctors, ratings) is readable anonymously</li>
 *   <li>patients ({@code /api/users}) require authentication</li>
 *   <li>everything under {@code /api/appointments} requires authentication</li>
 *   <li>approving an appointment additionally requires {@code ROLE_DOCTOR}
 *       <em>and</em> ownership of that appointment (checked in the service
 *       layer)</li>
 * </ul>
 *
 * <p>TODO (when you want token based clients): replace Basic with a JWT
 * resource server filter and add a login endpoint issuing the token.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Delegating encoder: stored hashes are prefixed with "{bcrypt}".
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			DaoAuthenticationProvider authenticationProvider) throws Exception {
		return http
			// Stateless API: there is no browser session and no form to protect.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authenticationProvider(authenticationProvider)
			.httpBasic(Customizer.withDefaults())
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/error").permitAll()
				// Public catalogue reads.
				.requestMatchers(HttpMethod.GET, "/api/services/**", "/api/doctors/**", "/api/ratings/**")
				.permitAll()
				// Patients are not public data.
				.requestMatchers("/api/users/**").authenticated()
				// Own appointments, pending/approved lists, approving: authenticated.
				.requestMatchers("/api/appointments/**").authenticated()
				.anyRequest().authenticated())
			.build();
	}

}
