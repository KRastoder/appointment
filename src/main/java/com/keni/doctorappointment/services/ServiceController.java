package com.keni.doctorappointment.services;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.services.dto.ServiceResponse;

/**
 * Placeholder endpoints for the {@code services} module (read-only).
 *
 * <p>TODO: catalogue CRUD and doctor-service assignment endpoints.</p>
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

	private final ServiceService serviceService;

	@GetMapping
	public List<ServiceResponse> findAll() {
		return serviceService.findAll();
	}

	@GetMapping("/{id}")
	public ServiceResponse getById(@PathVariable Long id) {
		return serviceService.getById(id);
	}

}
