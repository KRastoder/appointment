package com.keni.doctorappointment.doctors;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.keni.doctorappointment.common.dto.ChangePasswordRequest;
import com.keni.doctorappointment.doctors.dto.CreateDoctorRequest;
import com.keni.doctorappointment.doctors.dto.DoctorResponse;
import com.keni.doctorappointment.doctors.dto.UpdateDoctorRequest;
import com.keni.doctorappointment.security.AppUserPrincipal;

/**
 * Doctor endpoints.
 *
 * <pre>
 * GET    /api/doctors                    public catalogue
 * GET    /api/doctors/{id}               public, includes offered service ids
 * POST   /api/doctors                    ADMIN
 * PUT    /api/doctors/{id}               own profile or ADMIN
 * DELETE /api/doctors/{id}               ADMIN
 * POST   /api/doctors/{id}/services/{sid}     ADMIN: doctor offers a service
 * DELETE /api/doctors/{id}/services/{sid}     ADMIN: doctor stops offering it
 * </pre>
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

	/** Profile of the authenticated doctor. */
	@GetMapping("/me")
	public DoctorResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return doctorService.getMine(principal);
	}

	/** Password change for the authenticated doctor (current password required). */
	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		doctorService.changePassword(principal, request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public Map<String, Object> getById(@PathVariable Long id) {
		return Map.of("doctor", doctorService.getById(id), "serviceIds", doctorService.offeredServiceIds(id));
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
	}

	@PutMapping("/{id}")
	public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDoctorRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return doctorService.update(id, request, principal);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		doctorService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/services/{serviceId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> offerService(@PathVariable Long id, @PathVariable Long serviceId) {
		doctorService.offerService(id, serviceId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/services/{serviceId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> removeService(@PathVariable Long id, @PathVariable Long serviceId) {
		doctorService.removeService(id, serviceId);
		return ResponseEntity.noContent().build();
	}

}
