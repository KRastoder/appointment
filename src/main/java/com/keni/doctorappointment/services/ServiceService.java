package com.keni.doctorappointment.services;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.services.dto.CreateServiceRequest;
import com.keni.doctorappointment.services.dto.ServiceResponse;
import com.keni.doctorappointment.services.dto.UpdateServiceRequest;

/**
 * Service catalogue.
 *
 * <p>Reads are public; every mutation requires an ADMIN (enforced with
 * {@code @PreAuthorize} in {@link ServiceController}).</p>
 *
 * <p>Note: deleting a service that is already booked is rejected by the
 * foreign keys, which keeps appointment history intact.</p>
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

	private final ServiceRepository serviceRepository;

	@Transactional
	public ServiceResponse create(CreateServiceRequest request) {
		String name = request.name().trim();
		assertNameAvailable(name, null);

		Service service = new Service(name, request.description(), request.durationMinutes(), request.price());
		return ServiceResponse.from(serviceRepository.save(service));
	}

	@Transactional(readOnly = true)
	public List<ServiceResponse> findAll() {
		return serviceRepository.findAll().stream().map(ServiceResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public ServiceResponse getById(Long id) {
		return ServiceResponse.from(requireService(id));
	}

	@Transactional
	public ServiceResponse update(Long id, UpdateServiceRequest request) {
		Service service = requireService(id);

		if (request.name() != null) {
			String name = request.name().trim();
			assertNameAvailable(name, service.getId());
			service.setName(name);
		}
		if (request.description() != null) {
			service.setDescription(request.description());
		}
		if (request.durationMinutes() != null) {
			service.setDurationMinutes(request.durationMinutes());
		}
		if (request.price() != null) {
			service.setPrice(request.price());
		}

		return ServiceResponse.from(serviceRepository.save(service));
	}

	@Transactional
	public void delete(Long id) {
		serviceRepository.delete(requireService(id));
	}

	private Service requireService(Long id) {
		return serviceRepository.findById(id)
			.orElseThrow(() -> new NoSuchElementException("Service %d not found".formatted(id)));
	}

	private void assertNameAvailable(String name, Long ownServiceId) {
		serviceRepository.findByName(name)
			.filter(existing -> ownServiceId == null || !existing.getId().equals(ownServiceId))
			.ifPresent(existing -> {
				throw new IllegalStateException("A service named %s already exists".formatted(name));
			});
	}

}
