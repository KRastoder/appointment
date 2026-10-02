package com.keni.doctorappointment.services;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.services.dto.CreateServiceRequest;
import com.keni.doctorappointment.services.dto.ServiceResponse;
import com.keni.doctorappointment.services.dto.UpdateServiceRequest;

/**
 * Service catalogue endpoints.
 *
 * <pre>
 * GET    /api/services           public
 * GET    /api/services/{id}      public
 * POST   /api/services           ADMIN
 * PUT    /api/services/{id}      ADMIN
 * DELETE /api/services/{id}      ADMIN
 * </pre>
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

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ServiceResponse update(@PathVariable Long id, @Valid @RequestBody UpdateServiceRequest request) {
		return serviceService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		serviceService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
