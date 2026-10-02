package com.keni.doctorappointment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.keni.doctorappointment.security.JwtAuthenticationFilter;
import com.keni.doctorappointment.security.RestAuthenticationEntryPoint;

/**
 * Security configuration: stateless JWT authentication, three roles.
 *
 * <p>No OAuth2 / OpenID Connect: {@code /api/auth/login} checks the BCrypt
 * hash and issues a hand-rolled HS256 token pair, and
 * {@link JwtAuthenticationFilter} authenticates every request from the
 * {@code Authorization: Bearer ...} header.</p>
 *
 * <p>Roles:</p>
 * <ul>
 *   <li>{@code ROLE_USER} - patients, and staff accounts as well</li>
 *   <li>{@code ROLE_DOCTOR} - may approve his own appointments</li>
 *   <li>{@code ROLE_ADMIN} - manages the service catalogue and doctors</li>
 * </ul>
 *
 * <p>Public: login/refresh, patient registration and read access to the
 * catalogue. Everything else needs a token; mutations of the catalogue and of
 * doctor accounts need an ADMIN (see also {@code @PreAuthorize}).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Delegating encoder: stored hashes are prefixed with "{bcrypt}".
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
			RestAuthenticationEntryPoint entryPoint) throws Exception {

		return http
			// Stateless token API: no browser session and no form to protect.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/error").permitAll()
				// Authentication endpoints (logout is authenticated by the
				// refresh token in the request body, no Bearer token needed).
				.requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/register", "/api/auth/logout")
				.permitAll()
				// The doctor's own account endpoints need a token (must come
				// before the public GET /api/doctors/** rule below).
				.requestMatchers("/api/doctors/me", "/api/doctors/me/password").authenticated()
				// Public catalogue reads.
				.requestMatchers(HttpMethod.GET, "/api/services/**", "/api/doctors/**", "/api/ratings/**")
				.permitAll()
				// Everything else requires a valid access token.
				.anyRequest().authenticated())
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

}
