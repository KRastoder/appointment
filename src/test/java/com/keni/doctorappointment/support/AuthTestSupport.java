package com.keni.doctorappointment.support;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Helpers for API tests that need a signed-in caller.
 *
 * <p>Accounts are created through the real HTTP API and the token pair comes
 * from {@code POST /api/auth/login}, so tests walk the same path a client
 * does. The ADMIN account is the bootstrap one from
 * {@code app.bootstrap.admin.*}.</p>
 */
public abstract class AuthTestSupport {

	@Autowired
	protected TestRestTemplate restTemplate;

	/** Public patient self registration. */
	protected String registerPatient(String email, String password) {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register",
				Map.of("firstName", "Pat", "lastName", "Patient", "email", email, "password", password,
						"phoneNumber", "+49 30 000000"),
				String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException("Could not register " + email + ": " + response.getBody());
		}
		return email;
	}

	/** ADMIN creating a patient through {@code POST /api/users}. */
	protected String createPatientByAdmin(String adminEmail, String adminPassword, String email, String password) {
		String token = login(adminEmail, adminPassword).accessToken();
		HttpEntity<Map<String, Object>> body = new HttpEntity<>(Map.of("firstName", "Managed", "lastName", "Patient",
				"email", email, "password", password, "role", "PATIENT"), bearer(token));
		ResponseEntity<String> created = restTemplate.exchange("/api/users", HttpMethod.POST, body, String.class);
		if (!created.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException("Could not create " + email + ": " + created.getBody());
		}
		return email;
	}

	/** ADMIN creating a doctor through {@code POST /api/doctors}. */
	protected String createDoctorByAdmin(String adminEmail, String adminPassword, String email, String password,
			String specialization) {
		String token = login(adminEmail, adminPassword).accessToken();
		HttpEntity<Map<String, Object>> body = new HttpEntity<>(Map.of("firstName", "Doc", "lastName", "Tor",
				"email", email, "password", password, "specialization", specialization,
				"bio", "created by tests"), bearer(token));
		ResponseEntity<String> created = restTemplate.exchange("/api/doctors", HttpMethod.POST, body, String.class);
		if (!created.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException("Could not create doctor " + email + ": " + created.getBody());
		}
		return email;
	}

	/** ADMIN creating a catalogue service through {@code POST /api/services}. */
	protected Long createServiceByAdmin(String adminEmail, String adminPassword, String name) {
		String token = login(adminEmail, adminPassword).accessToken();
		HttpEntity<Map<String, Object>> body = new HttpEntity<>(Map.of("name", name, "description", "test service",
				"durationMinutes", 30, "price", 99.90), bearer(token));
		ResponseEntity<Map> created = restTemplate.exchange("/api/services", HttpMethod.POST, body, Map.class);
		if (!created.getStatusCode().is2xxSuccessful() || created.getBody() == null) {
			throw new IllegalStateException("Could not create service " + name + ": " + created.getBody());
		}
		return ((Number) created.getBody().get("id")).longValue();
	}

	protected TokenPair login(String email, String password) {
		ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
				Map.of("email", email, "password", password), Map.class);
		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new IllegalStateException("Login failed for " + email + ": " + response.getBody());
		}
		Map<String, Object> body = response.getBody();
		return new TokenPair((String) body.get("accessToken"), (String) body.get("refreshToken"));
	}

	protected HttpHeaders bearer(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	protected record TokenPair(String accessToken, String refreshToken) {
	}

}
