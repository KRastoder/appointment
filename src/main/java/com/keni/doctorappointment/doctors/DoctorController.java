package com.keni.doctorappointment.doctors;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.doctors.dto.DoctorResponse;

/**
 * Placeholder endpoints for the {@code doctors} module (read-only).
 *
 * <p>TODO: doctor CRUD, service assignment, search and average rating.</p>
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

	private final DoctorService doctorService;

	@GetMapping
	public List<DoctorResponse> findAll() {
		return doctorService.findAll();
	}

	@GetMapping("/{id}")
	public DoctorResponse getById(@PathVariable Long id) {
		return doctorService.getById(id);
	}

}
