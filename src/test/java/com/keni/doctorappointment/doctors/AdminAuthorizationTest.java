package com.keni.doctorappointment.doctors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.support.AuthTestSupport;

/**
 * The ADMIN role: managing the service catalogue, doctor accounts and the
 * services a doctor offers. Also verifies that patients and doctors cannot.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAuthorizationTest extends AuthTestSupport {

	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String PASSWORD = "Member123";

	private static final String ADMIN = "admin@doctor-appointment.local";

	private static final String ADMIN_PASSWORD = "Admin12345";

	@Autowired
	private DoctorRepository doctors;

	@Autowired
	private ServiceRepository services;

	@Test
	void serviceCatalogueIsWritableByAdminOnly() {
		String serviceName = "Admin service " + SEQ.incrementAndGet();
		Map<String, Object> body = Map.of("name", serviceName, "durationMinutes", 30, "price", 50);

		assertThat(call("/api/services", HttpMethod.POST, null, body).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);

		String patient = registerPatient(uniqueEmail("patient"), PASSWORD);
		assertThat(call("/api/services", HttpMethod.POST, patient, body).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		String doctor = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD, uniqueEmail("doctor"), PASSWORD, "Cardiology");
		assertThat(call("/api/services", HttpMethod.POST, doctor, body).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		// admin succeeds
		ResponseEntity<String> created = call("/api/services", HttpMethod.POST, ADMIN,
				Map.of("name", serviceName, "description", "by admin", "durationMinutes", 30, "price", 50));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		Long serviceId = idOf(created.getBody());

		assertThat(call("/api/services/" + serviceId, HttpMethod.PUT, ADMIN, Map.of("price", 60.00))
			.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(call("/api/services/" + serviceId, HttpMethod.DELETE, ADMIN, null).getStatusCode())
			.isEqualTo(HttpStatus.NO_CONTENT);

		// public reads keep working
		assertThat(call("/api/services", HttpMethod.GET, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void onlyAdminCreatesDoctorAccounts() {
		Map<String, Object> body = Map.of("firstName", "New", "lastName", "Doctor", "email",
				uniqueEmail("doctor"), "password", PASSWORD, "specialization", "Neurology");

		String patient = registerPatient(uniqueEmail("patient"), PASSWORD);
		assertThat(call("/api/doctors", HttpMethod.POST, patient, body).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		assertThat(call("/api/doctors", HttpMethod.POST, null, body).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);

		ResponseEntity<String> created = call("/api/doctors", HttpMethod.POST, ADMIN, body);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		String doctorEmail = emailOf(created.getBody());
		Long doctorId = idOf(created.getBody());

		// the new doctor can log in and edit his own profile
		assertThat(call("/api/doctors/" + doctorId, HttpMethod.PUT, doctorEmail, Map.of("bio", "hello"))
			.getStatusCode()).isEqualTo(HttpStatus.OK);

		// but nobody else's
		String other = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD, uniqueEmail("doctor"), PASSWORD, "Cardiology");
		assertThat(call("/api/doctors/" + doctorId, HttpMethod.PUT, other, Map.of("bio", "hack")).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		// only an admin deletes a doctor account
		assertThat(call("/api/doctors/" + doctorId, HttpMethod.DELETE, doctorEmail, null).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void adminAssignsServicesToDoctors() {
		String doctorEmail = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD, uniqueEmail("doctor"), PASSWORD,
				"Dermatology");
		Long doctorId = doctors.findByEmailIgnoreCase(doctorEmail).orElseThrow().getId();
		Long serviceId = createServiceByAdmin(ADMIN, ADMIN_PASSWORD, "Derm " + SEQ.incrementAndGet());

		assertThat(call("/api/doctors/" + doctorId + "/services/" + serviceId, HttpMethod.POST, ADMIN, null)
			.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> view = call("/api/doctors/" + doctorId, HttpMethod.GET, null, null);
		assertThat(view.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(view.getBody()).contains(serviceId.toString());

		// assigning twice is harmless (composite primary key would reject it)
		assertThat(call("/api/doctors/" + doctorId + "/services/" + serviceId, HttpMethod.POST, ADMIN, null)
			.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertThat(call("/api/doctors/" + doctorId + "/services/" + serviceId, HttpMethod.DELETE, ADMIN, null)
			.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertThat(call("/api/doctors/" + doctorId, HttpMethod.GET, null, null).getBody())
			.contains("\"serviceIds\":[]");
	}

	@Test
	void doctorsCannotAssignServicesAndPatientsCannotListUsers() {
		String doctorEmail = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD, uniqueEmail("doctor"), PASSWORD,
				"Cardiology");
		Long doctorId = doctors.findByEmailIgnoreCase(doctorEmail).orElseThrow().getId();
		Long serviceId = createServiceByAdmin(ADMIN, ADMIN_PASSWORD, "Service " + SEQ.incrementAndGet());

		assertThat(call("/api/doctors/" + doctorId + "/services/" + serviceId, HttpMethod.POST, doctorEmail, null)
			.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		String patient = registerPatient(uniqueEmail("patient"), PASSWORD);
		assertThat(call("/api/users", HttpMethod.GET, patient, null).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(call("/api/users", HttpMethod.GET, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void adminPromotesAndDemotesRoles() {
		String patient = createPatientByAdmin(ADMIN, ADMIN_PASSWORD, uniqueEmail("patient"), PASSWORD);

		ResponseEntity<String> me = call("/api/users/me", HttpMethod.GET, patient, null);
		assertThat(me.getBody()).contains("\"role\":\"PATIENT\"");
		Long userId = idOf(me.getBody());

		// a patient must not promote itself
		assertThat(call("/api/users/me", HttpMethod.PUT, patient, Map.of("role", "ADMIN")).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		assertThat(call("/api/users/" + userId, HttpMethod.PUT, ADMIN, Map.of("role", "ADMIN")).getStatusCode())
			.isEqualTo(HttpStatus.OK);

		assertThat(call("/api/users/" + userId, HttpMethod.GET, ADMIN, null).getBody())
			.contains("\"role\":\"ADMIN\"");

		// the promoted account now authenticates as ADMIN
		ResponseEntity<String> whoami = call("/api/auth/me", HttpMethod.GET, patient, null);
		assertThat(whoami.getBody()).contains("\"accountType\":\"ADMIN\"");
	}

	// --- helpers ---------------------------------------------------------

	private String uniqueEmail(String prefix) {
		return "admin-" + prefix + "-" + SEQ.incrementAndGet() + "@example.org";
	}

	/** Calls the API as the account behind {@code loginEmail}; null = anonymous. */
	private ResponseEntity<String> call(String path, HttpMethod method, String loginEmail,
			Map<String, Object> body) {
		HttpHeaders headers = new HttpHeaders();
		if (loginEmail != null) {
			String password = loginEmail.equals(ADMIN) ? ADMIN_PASSWORD : PASSWORD;
			headers.setBearerAuth(login(loginEmail, password).accessToken());
		}
		if (body != null) {
			headers.setContentType(MediaType.APPLICATION_JSON);
		}
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

	private Long idOf(String json) {
		return Long.valueOf(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
	}

	private String emailOf(String json) {
		return json.replaceAll(".*\"email\":\"([^\"]+)\".*", "$1");
	}

}
