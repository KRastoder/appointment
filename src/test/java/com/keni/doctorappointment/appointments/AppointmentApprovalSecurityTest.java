package com.keni.doctorappointment.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.services.Service;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.support.AuthTestSupport;
import com.keni.doctorappointment.support.PostgresTestSupport;
import com.keni.doctorappointment.users.User;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Authorisation of the appointment module over the real HTTP API: only the
 * doctor an appointment belongs to may approve it, and a doctor sees his own
 * pending and approved appointments.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppointmentApprovalSecurityTest extends PostgresTestSupport {

	private static final String PASSWORD = "Doctor123";

	private static final String ADMIN = "admin@doctor-appointment.local";

	private static final String ADMIN_PASSWORD = "Admin12345";

	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T10:00:00+01:00");

	@Autowired
	private DoctorRepository doctors;

	@Autowired
	private UserRepository users;

	@Autowired
	private ServiceRepository services;

	@Autowired
	private AppointmentRepository appointments;

	private String doctorOneEmail;

	private String doctorTwoEmail;

	private String patientEmail;

	private Appointment ownAppointment;

	private Appointment foreignAppointment;

	@BeforeEach
	void setUp() {
		int n = SEQ.incrementAndGet();
		String adminToken = login(ADMIN, ADMIN_PASSWORD).accessToken();

		doctorOneEmail = createDoctor(adminToken, "doctor-one-" + n + "@example.org");
		doctorTwoEmail = createDoctor(adminToken, "doctor-two-" + n + "@example.org");
		patientEmail = createPatientByAdmin(ADMIN, ADMIN_PASSWORD, "patient-" + n + "@example.org", PASSWORD);

		Doctor doctorOne = doctors.findByEmailIgnoreCase(doctorOneEmail).orElseThrow();
		Doctor doctorTwo = doctors.findByEmailIgnoreCase(doctorTwoEmail).orElseThrow();
		User patient = users.findByEmailIgnoreCase(patientEmail).orElseThrow();
		Service service = services
			.save(new Service("Consultation " + n, "test service", 30, new BigDecimal("99.00")));

		ownAppointment = appointments.save(new Appointment(patient, doctorOne, service, START,
				START.plusMinutes(30), AppointmentStatus.SCHEDULED, null));
		foreignAppointment = appointments.save(new Appointment(patient, doctorTwo, service, START.plusDays(1),
				START.plusDays(1).plusMinutes(30), AppointmentStatus.SCHEDULED, null));
	}

	private String createDoctor(String adminToken, String email) {
		ResponseEntity<String> created = post("/api/doctors", adminToken,
				Map.of("firstName", "Doc", "lastName", "Tor", "email", email, "password", PASSWORD,
						"specialization", "Cardiology"));
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return email;
	}

	// --- authentication --------------------------------------------------

	@Test
	void anonymousRequestToAppointmentsIsRejected() {
		assertThat(get("/api/appointments/me", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void invalidAndForgedTokensAreRejected() {
		assertThat(get("/api/appointments/me", "not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		String token = login(doctorOneEmail, PASSWORD).accessToken();
		String forged = token.substring(0, token.length() - 3) + "AAA";
		assertThat(get("/api/appointments/me", forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void wrongPasswordIsRejected() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login",
				Map.of("email", doctorOneEmail, "password", "wrong-password"), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void catalogueIsPublicButPatientsAreNot() {
		assertThat(get("/api/services", null).getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(get("/api/doctors", null).getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(get("/api/users", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(get("/api/users", login(patientEmail, PASSWORD).accessToken()).getStatusCode())
			.isEqualTo(HttpStatus.OK);
	}

	// --- /me ------------------------------------------------------------

	@Test
	void doctorSeesOnlyHisOwnAppointments() {
		ResponseEntity<String> response = get("/api/appointments/me", doctorOneEmail);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains(ownAppointment.getId().toString())
			.doesNotContain(foreignAppointment.getId().toString());
	}

	@Test
	void patientSeesHisOwnBookings() {
		ResponseEntity<String> response = get("/api/appointments/me", patientEmail);

		assertThat(response.getBody()).contains(ownAppointment.getId().toString())
			.contains(foreignAppointment.getId().toString());
	}

	@Test
	void foreignAppointmentIsNotReadable() {
		assertThat(get("/api/appointments/" + foreignAppointment.getId(), doctorOneEmail).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	// --- /me/pending and /me/approved ------------------------------------

	@Test
	void pendingContainsScheduledAndApprovedContainsConfirmed() {
		assertThat(get("/api/appointments/me/pending", doctorOneEmail).getBody())
			.contains(ownAppointment.getId().toString());
		assertThat(get("/api/appointments/me/approved", doctorOneEmail).getBody()).doesNotContain("\"id\"");

		ResponseEntity<String> approved = post("/api/appointments/" + ownAppointment.getId() + "/approve", null,
				null);
		assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(approved.getBody()).contains("\"status\":\"CONFIRMED\"");

		assertThat(get("/api/appointments/me/pending", doctorOneEmail).getBody()).doesNotContain("\"id\"");
		assertThat(get("/api/appointments/me/approved", doctorOneEmail).getBody())
			.contains(ownAppointment.getId().toString());
	}

	@Test
	void patientsCannotUseTheDoctorLists() {
		assertThat(get("/api/appointments/me/pending", patientEmail).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(get("/api/appointments/me/approved", patientEmail).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	// --- approve ---------------------------------------------------------

	@Test
	void onlyTheOwningDoctorCanApprove() {
		Long id = ownAppointment.getId();

		// patient: not a doctor
		assertThat(post("/api/appointments/" + id + "/approve", null, null).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(postAs(patientEmail, "/api/appointments/" + id + "/approve").getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);

		// another doctor: the appointment is not his -> 404, ids cannot be probed
		assertThat(postAs(doctorTwoEmail, "/api/appointments/" + id + "/approve").getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);

		// the owning doctor succeeds
		assertThat(postAs(doctorOneEmail, "/api/appointments/" + id + "/approve").getStatusCode())
			.isEqualTo(HttpStatus.OK);

		assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

	@Test
	void approvingTwiceIsAConflict() {
		assertThat(postAs(doctorOneEmail, "/api/appointments/" + ownAppointment.getId() + "/approve")
			.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(postAs(doctorOneEmail, "/api/appointments/" + ownAppointment.getId() + "/approve")
			.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	// --- helpers: one token per account, reused by every test ------------

	private ResponseEntity<String> get(String path, String loginEmail) {
		return exchange(path, HttpMethod.GET, loginEmail == null ? null : tokenOf(loginEmail), null);
	}

	private ResponseEntity<String> postAs(String loginEmail, String path) {
		return exchange(path, HttpMethod.POST, tokenOf(loginEmail), null);
	}

	private ResponseEntity<String> post(String path, String accessToken, Map<String, Object> body) {
		return exchange(path, HttpMethod.POST, accessToken, body);
	}

	private String tokenOf(String loginEmail) {
		return login(loginEmail, PASSWORD).accessToken();
	}

	private ResponseEntity<String> exchange(String path, HttpMethod method, String accessToken,
			Map<String, Object> body) {
		HttpHeaders headers = new HttpHeaders();
		if (accessToken != null) {
			headers.setBearerAuth(accessToken);
		}
		if (body != null) {
			headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		}
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

}
