package com.keni.doctorappointment.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure beans shared by the feature modules.
 *
 * <p>A single {@link Clock} bean exists so that time-dependent booking rules
 * (availability windows, cancellation deadlines, the "has the appointment
 * ended yet" checks behind completing or marking a no-show) can be tested
 * deterministically instead of calling {@code OffsetDateTime.now()} directly.
 * Every service that compares against "now" takes this bean; see
 * {@code AppointmentLifecycleTest} for a test that moves it.</p>
 */
@Configuration
public class TimeConfig {

	/**
	 * Clock used for all domain time calculations. Defaults to the system zone
	 * of the application; override the JVM zone (or replace this bean) to pin
	 * the clinic's local time.
	 */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

}
