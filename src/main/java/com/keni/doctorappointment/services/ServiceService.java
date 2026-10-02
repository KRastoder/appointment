package com.keni.doctorappointment.services;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.services.dto.ServiceResponse;

/**
 * Placeholder service for the {@code services} module (catalogue reads).
 *
 * <p>{@code org.springframework.stereotype.Service} is referenced by its fully
 * qualified name because this package already contains the domain class
 * {@link Service}.</p>
 *
 * <p>TODO (business logic, intentionally not implemented here): catalogue
 * CRUD, service activation/deactivation, which doctor may offer which
 * service.</p>
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

	private final ServiceRepository serviceRepository;

	@Transactional(readOnly = true)
	public List<ServiceResponse> findAll() {
		return serviceRepository.findAll().stream().map(ServiceResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public ServiceResponse getById(Long id) {
		return serviceRepository.findById(id)
				.map(ServiceResponse::from)
				.orElseThrow(() -> new NoSuchElementException("Service %d not found".formatted(id)));
	}

}
