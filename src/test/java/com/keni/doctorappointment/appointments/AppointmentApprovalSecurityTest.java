package com.keni.doctorappointment.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.keni.doctorappointment.doctors.Doctor;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.services.Service;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.support.PostgresTestSupport;
import com.keni.doctorappointment.users.User;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Verifies the authentication / authorisation rules of the appointment module:
 * only the doctor an appointment belongs to may approve it, and a doctor can
 * list his own pending and approved appointments.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentApprovalSecurityTest extends PostgresTestSupport {

	private static final String PASSWORD = "s3cret-pass";

	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T10:00:00+01:00");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private DoctorRepository doctors;

	@Autowired
	private UserRepository users;

	@Autowired
	private ServiceRepository services;

	@Autowired
	private AppointmentRepository appointments;

	private Doctor doctorOne;

	private Doctor doctorTwo;

	private User patient;

	private Service service;

	private Appointment ownAppointment;

	private Appointment foreignAppointment;

	@BeforeEach
	void setUp() {
		int n = SEQ.incrementAndGet();

		doctorOne = doctors.save(doctor("doctor-one-" + n));
		doctorTwo = doctors.save(doctor("doctor-two-" + n));
		patient = users.save(new User("Pat", "Patient-" + n, "patient-" + n + "@example.org",
				passwordEncoder.encode(PASSWORD), null));
		service = services.save(new Service("Consultation " + n, "test service", 30, new BigDecimal("99.00")));

		ownAppointment = appointments.save(new Appointment(patient, doctorOne, service, START,
				START.plusMinutes(30), AppointmentStatus.SCHEDULED, null));
		foreignAppointment = appointments.save(new Appointment(patient, doctorTwo, service, START.plusDays(1),
				START.plusDays(1).plusMinutes(30), AppointmentStatus.SCHEDULED, null));
	}

	private Doctor doctor(String email) {
		return new Doctor("Doc", email, email + "@example.org", passwordEncoder.encode(PASSWORD), null,
				"Cardiology", null);
	}

	// --- authentication --------------------------------------------------

	@Test
	void anonymousRequestToAppointmentsIsRejected() throws Exception {
		mockMvc.perform(get("/api/appointments/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		mockMvc.perform(get("/api/appointments/me").with(httpBasic(doctorOne.getEmail(), "wrong")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void catalogueIsPublicButPatientsAreNot() throws Exception {
		mockMvc.perform(get("/api/services")).andExpect(status().isOk());
		mockMvc.perform(get("/api/doctors")).andExpect(status().isOk());

		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/users").with(httpBasic(patient.getEmail(), PASSWORD)))
			.andExpect(status().isOk());
	}

	// --- /me ------------------------------------------------------------

	@Test
	void doctorSeesOnlyHisOwnAppointments() throws Exception {
		mockMvc.perform(get("/api/appointments/me").with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(ownAppointment.getId()))
			.andExpect(jsonPath("$[0].status").value("SCHEDULED"));
	}

	@Test
	void patientSeesHisOwnBookings() throws Exception {
		mockMvc.perform(get("/api/appointments/me").with(httpBasic(patient.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void foreignAppointmentIsNotReadable() throws Exception {
		mockMvc.perform(get("/api/appointments/" + foreignAppointment.getId())
				.with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isForbidden());
	}

	// --- /me/pending and /me/approved ------------------------------------

	@Test
	void pendingContainsScheduledAndApprovedContainsConfirmed() throws Exception {
		mockMvc.perform(get("/api/appointments/me/pending").with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].status").value("SCHEDULED"));

		mockMvc.perform(get("/api/appointments/me/approved").with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));

		MvcResult approved = mockMvc
			.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andReturn();

		assertThat(approved.getResponse().getContentAsString()).contains("CONFIRMED");

		mockMvc.perform(get("/api/appointments/me/pending").with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/appointments/me/approved").with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void patientsCannotUseTheDoctorLists() throws Exception {
		mockMvc.perform(get("/api/appointments/me/pending").with(httpBasic(patient.getEmail(), PASSWORD)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/appointments/me/approved").with(httpBasic(patient.getEmail(), PASSWORD)))
			.andExpect(status().isForbidden());
	}

	// --- approve ---------------------------------------------------------

	@Test
	void onlyTheOwningDoctorCanApprove() throws Exception {
		// patient: not a doctor
		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(patient.getEmail(), PASSWORD))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden());

		// another doctor: the appointment is not his
		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(doctorTwo.getEmail(), PASSWORD)))
			.andExpect(status().isNotFound());

		// anonymous
		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve"))
			.andExpect(status().isUnauthorized());

		// the owning doctor succeeds
		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"));

		assertThat(appointments.findById(ownAppointment.getId()).orElseThrow().getStatus())
			.isEqualTo(AppointmentStatus.CONFIRMED);
	}

	@Test
	void approvingTwiceIsAConflict() throws Exception {
		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/appointments/" + ownAppointment.getId() + "/approve")
				.with(httpBasic(doctorOne.getEmail(), PASSWORD)))
			.andExpect(status().isConflict());
	}

}
