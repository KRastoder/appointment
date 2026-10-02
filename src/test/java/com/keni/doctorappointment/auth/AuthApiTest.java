package com.keni.doctorappointment.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.keni.doctorappointment.support.AuthTestSupport;

/**
 * Registration, login, refresh and logout over the real HTTP API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiTest extends AuthTestSupport {

	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String PASSWORD = "Patient123";

	private static final String BOOTSTRAP_ADMIN = "admin@doctor-appointment.local";

	private static final String BOOTSTRAP_ADMIN_PASSWORD = "Admin12345";

	@Autowired
	private TestRestTemplate template;

	private String uniqueEmail() {
		return "auth-patient-" + SEQ.incrementAndGet() + "@example.org";
	}

	@Test
	void publicRegistrationThenLogin() {
		String email = registerPatient(uniqueEmail(), PASSWORD);

		ResponseEntity<Map> login = template.postForEntity("/api/auth/login",
				Map.of("email", email, "password", PASSWORD), Map.class);

		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(login.getHeaders().getFirst("WWW-Authenticate")).isNull();
		Map<String, Object> body = login.getBody();
		assertThat(body).containsKeys("accessToken", "refreshToken", "tokenType", "expiresIn", "accountType");
		assertThat(body.get("tokenType")).isEqualTo("Bearer");
		assertThat(body.get("accountType")).isEqualTo("PATIENT");
		assertThat((String) body.get("accessToken")).contains(".");
	}

	@Test
	void loginIsCaseInsensitiveAndNormalisesTheEmail() {
		String email = registerPatient(uniqueEmail(), PASSWORD);

		ResponseEntity<Map> login = template.postForEntity("/api/auth/login",
				Map.of("email", email.toUpperCase(), "password", PASSWORD), Map.class);

		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(login.getBody().get("email")).isEqualTo(email);
	}

	@Test
	void wrongPasswordAndUnknownEmailAreRejected() {
		String email = registerPatient(uniqueEmail(), PASSWORD);

		assertThat(template.postForEntity("/api/auth/login", Map.of("email", email, "password", "nope123"),
				String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		assertThat(template.postForEntity("/api/auth/login",
				Map.of("email", "nobody@example.org", "password", PASSWORD), String.class).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void passwordRulesAreEnforced() {
		ResponseEntity<String> tooShort = template.postForEntity("/api/auth/register",
				Map.of("firstName", "A", "lastName", "B", "email", uniqueEmail(), "password", "Ab1"), String.class);
		assertThat(tooShort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		ResponseEntity<String> noDigit = template.postForEntity("/api/auth/register",
				Map.of("firstName", "A", "lastName", "B", "email", uniqueEmail(), "password", "abcdefghij"),
				String.class);
		assertThat(noDigit.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		ResponseEntity<String> badEmail = template.postForEntity("/api/auth/register",
				Map.of("firstName", "A", "lastName", "B", "email", "not-an-email", "password", "Abcdef123"),
				String.class);
		assertThat(badEmail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void emailIsUniqueCaseInsensitivelyAcrossPatientsAndDoctors() {
		String email = registerPatient(uniqueEmail(), PASSWORD);

		ResponseEntity<String> duplicate = template.postForEntity("/api/auth/register",
				Map.of("firstName", "Copy", "lastName", "Cat", "email", email.toUpperCase(), "password", PASSWORD),
				String.class);
		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		// a patient address must not be usable for a doctor account either
		HttpEntity<Map<String, Object>> doctor = new HttpEntity<>(
				Map.of("firstName", "Doc", "lastName", "Tor", "email", email.toUpperCase(), "password", PASSWORD,
						"specialization", "Cardiology"),
				bearer(login(BOOTSTRAP_ADMIN, BOOTSTRAP_ADMIN_PASSWORD).accessToken()));
		ResponseEntity<String> doctorResponse = template.exchange("/api/doctors", HttpMethod.POST, doctor,
				String.class);
		assertThat(doctorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void publicRegistrationCanNotCreateAdmins() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = template.exchange("/api/auth/register", HttpMethod.POST,
				new HttpEntity<>(Map.of("firstName", "Sneaky", "lastName", "Admin", "email", uniqueEmail(),
						"password", PASSWORD, "role", "ADMIN"), headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).doesNotContain("ADMIN");
	}

	@Test
	void meReportsTheAuthenticatedAccount() {
		String email = registerPatient(uniqueEmail(), PASSWORD);
		String token = login(email, PASSWORD).accessToken();

		ResponseEntity<Map> me = template.exchange("/api/auth/me", HttpMethod.GET,
				new HttpEntity<>(bearer(token)), Map.class);

		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(me.getBody()).containsEntry("email", email).containsEntry("accountType", "PATIENT");
	}

	@Test
	void accessTokenWorksAsBearerTokenAndGarbageDoesNot() {
		String email = registerPatient(uniqueEmail(), PASSWORD);
		String token = login(email, PASSWORD).accessToken();

		assertThat(template.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class)
			.getStatusCode()).isEqualTo(HttpStatus.OK);

		HttpHeaders forged = new HttpHeaders();
		forged.setBearerAuth(token.substring(0, token.length() - 3) + "AAA");
		assertThat(template.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(forged), String.class)
			.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		HttpHeaders none = new HttpHeaders();
		assertThat(template.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(none), String.class)
			.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void refreshRotatesAndLogoutRevokes() {
		String email = registerPatient(uniqueEmail(), PASSWORD);
		TokenPair first = login(email, PASSWORD);

		ResponseEntity<Map> refreshed = template.postForEntity("/api/auth/refresh",
				Map.of("refreshToken", first.refreshToken()), Map.class);
		assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
		String secondAccess = (String) refreshed.getBody().get("accessToken");
		String secondRefresh = (String) refreshed.getBody().get("refreshToken");
		assertThat(secondAccess).isNotBlank();
		assertThat(secondRefresh).isNotEqualTo(first.refreshToken());

		// the refresh token is single use
		assertThat(template.postForEntity("/api/auth/refresh", Map.of("refreshToken", first.refreshToken()),
				String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		// logout revokes the current refresh token
		assertThat(template.postForEntity("/api/auth/logout", Map.of("refreshToken", secondRefresh), String.class)
			.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertThat(template.postForEntity("/api/auth/refresh", Map.of("refreshToken", secondRefresh), String.class)
			.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void accessTokenCannotBeUsedAsRefreshToken() {
		String email = registerPatient(uniqueEmail(), PASSWORD);
		TokenPair pair = login(email, PASSWORD);

		assertThat(template.postForEntity("/api/auth/refresh", Map.of("refreshToken", pair.accessToken()),
				String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void bootstrapAdminCanLogIn() {
		ResponseEntity<Map> login = template.postForEntity("/api/auth/login",
				Map.of("email", BOOTSTRAP_ADMIN, "password", BOOTSTRAP_ADMIN_PASSWORD), Map.class);

		assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(login.getBody()).containsEntry("accountType", "ADMIN");

		String token = (String) login.getBody().get("accessToken");
		ResponseEntity<Map> me = template.exchange("/api/auth/me", HttpMethod.GET,
				new HttpEntity<>(bearer(token)), Map.class);
		assertThat(me.getBody().get("roles")).asString().contains("ADMIN");
	}

	@Test
	void passwordChangeRequiresTheCurrentPassword() {
		String email = registerPatient(uniqueEmail(), PASSWORD);
		String token = login(email, PASSWORD).accessToken();

		HttpEntity<Map<String, Object>> wrong = new HttpEntity<>(Map.of("currentPassword", "wrong1234",
				"newPassword", "Another123"), bearer(token));
		assertThat(template.exchange("/api/users/me/password", HttpMethod.PUT, wrong, String.class).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);

		HttpEntity<Map<String, Object>> right = new HttpEntity<>(Map.of("currentPassword", PASSWORD,
				"newPassword", "Another123"), bearer(token));
		assertThat(template.exchange("/api/users/me/password", HttpMethod.PUT, right, String.class).getStatusCode())
			.isEqualTo(HttpStatus.NO_CONTENT);

		// new password works, old one does not
		assertThat(template.postForEntity("/api/auth/login", Map.of("email", email, "password", "Another123"),
				String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(template.postForEntity("/api/auth/login", Map.of("email", email, "password", PASSWORD),
				String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
