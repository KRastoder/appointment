package com.keni.doctorappointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Doctor Appointment Booking backend.
 *
 * <p>The code base is organised package-by-feature
 * ({@code users}, {@code doctors}, {@code services}, {@code doctorservices},
 * {@code appointments}, {@code ratings}) instead of layer-by-layer
 * ({@code entity/}, {@code repository/}, ...), so every feature keeps its
 * model, repository, service and web layer close together.</p>
 */
@SpringBootApplication
public class DoctorAppointmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctorAppointmentApplication.class, args);
	}

}
