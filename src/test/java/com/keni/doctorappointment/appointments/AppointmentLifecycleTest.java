package com.keni.doctorappointment.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.ratings.Rating;
import com.keni.doctorappointment.ratings.RatingRepository;
import com.keni.doctorappointment.services.Service;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.support.AuthTestSupport;
import com.keni.doctorappointment.support.FastTestConfig;
import com.keni.doctorappointment.support.MutableClock;
import com.keni.doctorappointment.users.UserRepository;

/**
 * The part of the appointment lifecycle that only happens once time has moved
 * on: completing a finished appointment, marking a no-show and rating a
 * completed visit.
 *
 * <p>Everything is driven through the public HTTP API. The trick is the
 * {@link MutableClock} that replaces the application clock for this test: the
 * appointment is booked in the future and "now" is then moved past its end
 * time. No row is edited behind the API's back, so these tests exercise the
 * same transitions a clinic performs the following day - and they finish in
 * milliseconds instead of waiting for the appointment to elapse.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ FastTestConfig.class, AppointmentLifecycleTest.TestClockConfig.class })
class AppointmentLifecycleTest extends AuthTestSupport {

	private static final String ADMIN = "admin@doctor-appointment.local";

	private static final String ADMIN_PASSWORD = "Admin12345";

	private static final String PASSWORD = "Doctor123";

	/** The clock's zone is pinned so weekly availability is predictable. */
	private static final ZoneId ZONE = ZoneId.of("UTC");

	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired
	private MutableClock clock;

	@Autowired
	private DoctorRepository doctors;

	@Autowired
	private UserRepository users;

	@Autowired
	private ServiceRepository services;

	@Autowired
	private AppointmentRepository appointments;

	@Autowired
	private RatingRepository ratings;

	private String doctorEmail;

	private String patientEmail;

	private long doctorId;

	private long serviceId;

	/** Midnight of the day the appointments are booked on. */
	private OffsetDateTime day;

	@BeforeEach
	void setUp() {
		// "Tomorrow" keeps this test valid forever and, more importantly, keeps
		// freshly issued JWTs from looking expired (the token service validates
		// expiry against the real clock, not against this one).
		day = LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
		clock.set(day.plusHours(9).toInstant()); // 09:00, appointments at 10:00 and 11:00

		int n = SEQ.incrementAndGet();
		doctorEmail = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD, "lifecycle-doc-" + n + "@example.org", PASSWORD,
				"Cardiology");
		patientEmail = createPatientByAdmin(ADMIN, ADMIN_PASSWORD, "lifecycle-pat-" + n + "@example.org", PASSWORD);

		doctorId = doctors.findByEmailIgnoreCase(doctorEmail).orElseThrow().getId();
		serviceId = services.save(new Service("Lifecycle " + n, "test service", 30, new BigDecimal("99.00")))
			.getId();

		// A doctor must offer the service before a patient can book it...
		assertThat(exchange("/api/doctors/" + doctorId + "/services/" + serviceId, HttpMethod.POST, ADMIN,
				ADMIN_PASSWORD, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		// ...and must be working on the day the appointment is booked.
		assertThat(exchange("/api/doctors/me/availability", HttpMethod.POST, doctorEmail, PASSWORD,
				body("dayOfWeek", day.getDayOfWeek().getValue(), "startTime", "00:00:00", "endTime", "23:59:00"))
			.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	// --- complete --------------------------------------------------------

	@Test
	void appointmentCannotBeCompletedBeforeItHasEnded() {
		Long id = bookAndApprove();

		ResponseEntity<String> tooEarly = postAs(doctorEmail, "/api/appointments/" + id + "/complete");
		assertThat(tooEarly.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(tooEarly.getBody()).contains("Appointment has not ended yet");
	}

	@Test
	void owningDoctorCompletesTheAppointmentOnceItHasEnded() {
		Long id = bookAndApprove();

		clock.advance(Duration.ofHours(3)); // 12:00, appointment ran 11:00-11:30

		ResponseEntity<String> completed = postAs(doctorEmail, "/api/appointments/" + id + "/complete");
		assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(completed.getBody()).contains("\"status\":\"COMPLETED\"");
		assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
	}

	@Test
	void completingTwiceIsAConflict() {
		Long id = bookAndApprove();
		clock.advance(Duration.ofHours(3));

		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/complete").getStatusCode())
			.isEqualTo(HttpStatus.OK);
		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/complete").getStatusCode())
			.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void patientsCannotCompleteAndOtherDoctorsCannotEither() {
		Long id = bookAndApprove();
		clock.advance(Duration.ofHours(3));

		String other = createDoctorByAdmin(ADMIN, ADMIN_PASSWORD,
				"lifecycle-other-" + SEQ.incrementAndGet() + "@example.org", PASSWORD, "Neurology");

		assertThat(postAs(patientEmail, "/api/appointments/" + id + "/complete").getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(postAs(other, "/api/appointments/" + id + "/complete").getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	// --- no-show ---------------------------------------------------------

	@Test
	void noShowCannotBeMarkedBeforeTheAppointmentHasEnded() {
		Long id = bookAndApprove();

		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/no-show").getStatusCode())
			.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void doctorMarksNoShowAfterTheAppointmentHasEnded() {
		Long id = bookAndApprove();
		clock.advance(Duration.ofHours(3));

		ResponseEntity<String> noShow = postAs(doctorEmail, "/api/appointments/" + id + "/no-show");
		assertThat(noShow.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(noShow.getBody()).contains("\"status\":\"NO_SHOW\"");
		assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
	}

	@Test
	void cancelledAppointmentCannotBeTurnedIntoANoShow() {
		Long id = bookAndApprove();

		assertThat(post(doctorEmail, "/api/appointments/" + id + "/cancel", body("reason", "test"))
			.getStatusCode()).isEqualTo(HttpStatus.OK);
		clock.advance(Duration.ofHours(3));

		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/no-show").getStatusCode())
			.isEqualTo(HttpStatus.CONFLICT);
	}

	// --- ratings ---------------------------------------------------------

	@Test
	void patientRatesTheDoctorAfterACompletedVisit() {
		Long id = bookAndApprove();
		clock.advance(Duration.ofHours(3));
		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/complete").getStatusCode())
			.isEqualTo(HttpStatus.OK);

		ResponseEntity<String> rating = post(patientEmail, "/api/ratings",
				body("appointmentId", id, "score", 4, "comment", "good doctor"));
		assertThat(rating.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(rating.getBody()).contains("\"score\":4");

		Rating stored = ratings.findByAppointment_Id(id).orElseThrow();
		assertThat(stored.getScore()).isEqualTo((short) 4);
		assertThat(stored.getUser().getId()).isEqualTo(users.findByEmailIgnoreCase(patientEmail).orElseThrow().getId());
	}

	@Test
	void onlyCompletedAppointmentsCanBeRated() {
		Long id = bookAndApprove();

		ResponseEntity<String> tooEarly = post(patientEmail, "/api/ratings", body("appointmentId", id, "score", 5));
		assertThat(tooEarly.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(tooEarly.getBody()).contains("Can only rate completed appointments");
	}

	@Test
	void oneRatingPerAppointmentAndNotByStrangers() {
		String stranger = createPatientByAdmin(ADMIN, ADMIN_PASSWORD,
				"lifecycle-stranger-" + SEQ.incrementAndGet() + "@example.org", PASSWORD);

		// Booked before time moves, at a slot that does not overlap 11:00-11:30.
		Long mine = bookAndApprove();
		Long theirs = book(stranger, day.plusHours(10));

		clock.advance(Duration.ofHours(3));
		postAs(doctorEmail, "/api/appointments/" + mine + "/complete");
		postAs(doctorEmail, "/api/appointments/" + theirs + "/complete");

		assertThat(post(patientEmail, "/api/ratings", body("appointmentId", mine, "score", 5)).getStatusCode())
			.isEqualTo(HttpStatus.CREATED);

		ResponseEntity<String> again = post(patientEmail, "/api/ratings",
				body("appointmentId", mine, "score", 1, "comment", "changed my mind"));
		assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(again.getBody()).contains("Rating for this appointment already exists");

		// the rejected attempt must not have overwritten the stored score
		assertThat(ratings.findByAppointment_Id(mine).orElseThrow().getScore()).isEqualTo((short) 5);

		// the stranger cannot rate somebody else's visit
		assertThat(post(stranger, "/api/ratings", body("appointmentId", mine, "score", 1)).getStatusCode())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void scoreOutsideOneToFiveIsRejected() {
		Long id = bookAndApprove();
		clock.advance(Duration.ofHours(3));
		postAs(doctorEmail, "/api/appointments/" + id + "/complete");

		assertThat(post(patientEmail, "/api/ratings", body("appointmentId", id, "score", 0)).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(post(patientEmail, "/api/ratings", body("appointmentId", id, "score", 6)).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	// --- helpers ---------------------------------------------------------

	/** Books at 11:00 and has the doctor approve it; the clock is at 09:00. */
	private Long bookAndApprove() {
		Long id = book(patientEmail, day.plusHours(11));
		assertThat(postAs(doctorEmail, "/api/appointments/" + id + "/approve").getStatusCode())
			.isEqualTo(HttpStatus.OK);
		return id;
	}

	private Long book(String email, OffsetDateTime start) {
		ResponseEntity<Map> booked = restTemplate.exchange("/api/appointments", HttpMethod.POST,
				new HttpEntity<>(body("doctorId", doctorId, "serviceId", serviceId, "startTime", start.toString()),
						jsonHeaders(login(email, PASSWORD).accessToken())),
				Map.class);
		assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return ((Number) booked.getBody().get("id")).longValue();
	}

	private ResponseEntity<String> postAs(String loginEmail, String path) {
		return exchange(path, HttpMethod.POST, loginEmail, PASSWORD, null);
	}

	private ResponseEntity<String> post(String loginEmail, String path, Map<String, Object> body) {
		return exchange(path, HttpMethod.POST, loginEmail, PASSWORD, body);
	}

	private ResponseEntity<String> exchange(String path, HttpMethod method, String loginEmail, String password,
			Map<String, Object> body) {
		return restTemplate.exchange(path, method,
				new HttpEntity<>(body, jsonHeaders(login(loginEmail, password).accessToken())), String.class);
	}

	private static HttpHeaders jsonHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private static Map<String, Object> body(Object... keyValues) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return map;
	}

	/**
	 * Replaces the application clock for this test only. {@code @Primary} wins
	 * over the production bean without needing bean-definition overriding.
	 */
	@TestConfiguration
	static class TestClockConfig {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant(), ZONE);
		}

	}

}