package com.keni.doctorappointment.doctors;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.doctors.dto.DoctorResponse;

/**
 * Placeholder service for the {@code doctors} module (reads only).
 *
 * <p>TODO (business logic, intentionally NOT implemented here):</p>
 * <ul>
 *   <li>doctor CRUD and profile management</li>
 *   <li>assigning/removing the services a doctor offers</li>
 *   <li>doctor search and filtering by specialization</li>
 *   <li>average rating computed from the {@code ratings} table</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DoctorService {

	private final DoctorRepository doctorRepository;

	@Transactional(readOnly = true)
	public List<DoctorResponse> findAll() {
		return doctorRepository.findAll().stream().map(DoctorResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public DoctorResponse getById(Long id) {
		return doctorRepository.findById(id)
				.map(DoctorResponse::from)
				.orElseThrow(() -> new NoSuchElementException("Doctor %d not found".formatted(id)));
	}

}
