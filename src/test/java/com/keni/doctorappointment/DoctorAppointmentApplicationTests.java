package com.keni.doctorappointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.keni.doctorappointment.appointments.AppointmentRepository;
import com.keni.doctorappointment.doctors.DoctorRepository;
import com.keni.doctorappointment.doctorservices.DoctorServiceRepository;
import com.keni.doctorappointment.ratings.RatingRepository;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.support.PostgresTestSupport;
import com.keni.doctorappointment.users.UserRepository;

/**
 * Boots the whole monolith against PostgreSQL.
 *
 * <p>Passing this test proves that</p>
 * <ul>
 *   <li>every Flyway migration applies cleanly, and</li>
 *   <li>every JPA entity matches the schema, because Hibernate is configured
 *       with {@code ddl-auto=validate} and fails fast on any mismatch.</li>
 * </ul>
 */
@SpringBootTest
class DoctorAppointmentApplicationTests extends PostgresTestSupport {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Clock clock;

	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
		assertThat(clock).isNotNull();
	}

	@Test
	void repositoriesAreRegistered() {
		assertThat(context.getBean(UserRepository.class)).isNotNull();
		assertThat(context.getBean(DoctorRepository.class)).isNotNull();
		assertThat(context.getBean(ServiceRepository.class)).isNotNull();
		assertThat(context.getBean(DoctorServiceRepository.class)).isNotNull();
		assertThat(context.getBean(AppointmentRepository.class)).isNotNull();
		assertThat(context.getBean(RatingRepository.class)).isNotNull();
	}

}
