package com.keni.doctorappointment.doctors;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keni.doctorappointment.doctors.dto.CreateDoctorRequest;
import com.keni.doctorappointment.doctors.dto.DoctorResponse;
import com.keni.doctorappointment.doctors.dto.UpdateDoctorRequest;
import com.keni.doctorappointment.doctorservices.DoctorServiceId;
import com.keni.doctorappointment.doctorservices.DoctorServiceRepository;
import com.keni.doctorappointment.security.AppUserPrincipal;
import com.keni.doctorappointment.services.ServiceRepository;
import com.keni.doctorappointment.users.UserRepository;
import com.keni.doctorappointment.users.UserService;

/**
 * Doctor accounts and the services they offer.
 *
 * <p>Creating, deleting and assigning services is reserved for an ADMIN;
 * a doctor may edit their own profile. The e-mail check spans patients and
 * doctors so that one address can never be two accounts.</p>
 */
@Service
@RequiredArgsConstructor
public class DoctorService {

	private final DoctorRepository doctorRepository;

	private final DoctorServiceRepository doctorServiceRepository;

	private final ServiceRepository serviceRepository;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	@Transactional
	public DoctorResponse create(CreateDoctorRequest request) {
		String email = UserService.normalise(request.email());
		assertEmailAvailable(email, null);

		Doctor doctor = new Doctor(request.firstName(), request.lastName(), email,
				passwordEncoder.encode(request.password()), request.phoneNumber(), request.specialization(),
				request.bio());
		return DoctorResponse.from(doctorRepository.save(doctor));
	}

	@Transactional(readOnly = true)
	public List<DoctorResponse> findAll() {
		return doctorRepository.findAll().stream().map(DoctorResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public DoctorResponse getById(Long id) {
		return DoctorResponse.from(requireDoctor(id));
	}

	/** Profile update: own profile, or any profile when the caller is an ADMIN. */
	@Transactional
	public DoctorResponse update(Long id, UpdateDoctorRequest request, AppUserPrincipal principal) {
		assertCanManage(principal, id);
		Doctor doctor = requireDoctor(id);

		if (request.firstName() != null) {
			doctor.setFirstName(request.firstName());
		}
		if (request.lastName() != null) {
			doctor.setLastName(request.lastName());
		}
		if (request.phoneNumber() != null) {
			doctor.setPhoneNumber(request.phoneNumber());
		}
		if (request.specialization() != null) {
			doctor.setSpecialization(request.specialization());
		}
		if (request.bio() != null) {
			doctor.setBio(request.bio());
		}
		if (request.email() != null) {
			String email = UserService.normalise(request.email());
			assertEmailAvailable(email, doctor.getId());
			doctor.setEmail(email);
		}

		return DoctorResponse.from(doctorRepository.save(doctor));
	}

	@Transactional
	public void delete(Long id) {
		doctorRepository.delete(requireDoctor(id));
	}

	/** Ids of the services this doctor offers. */
	@Transactional(readOnly = true)
	public List<Long> offeredServiceIds(Long doctorId) {
		return doctorServiceRepository.findByDoctor_IdOrderByService_Id(doctorId).stream()
			.map(doctorService -> doctorService.getId().getServiceId())
			.toList();
	}

	/** Lets a doctor offer one more service (no-op if already assigned). */
	@Transactional
	public void offerService(Long doctorId, Long serviceId) {
		Doctor doctor = requireDoctor(doctorId);
		serviceRepository.findById(serviceId)
			.orElseThrow(() -> new NoSuchElementException("Service %d not found".formatted(serviceId)));

		if (!doctorServiceRepository.existsById(new DoctorServiceId(doctorId, serviceId))) {
			// Fully qualified: this class shares its name with the join entity.
			doctorServiceRepository.save(new com.keni.doctorappointment.doctorservices.DoctorService(doctor,
					serviceRepository.getReferenceById(serviceId)));
		}
	}

	@Transactional
	public void removeService(Long doctorId, Long serviceId) {
		doctorServiceRepository.findById(new DoctorServiceId(doctorId, serviceId))
			.ifPresent(doctorServiceRepository::delete);
	}

	// --- helpers -------------------------------------------------------

	private Doctor requireDoctor(Long id) {
		return doctorRepository.findById(id)
			.orElseThrow(() -> new NoSuchElementException("Doctor %d not found".formatted(id)));
	}

	private void assertCanManage(AppUserPrincipal principal, Long targetId) {
		boolean self = principal.isDoctor() && principal.id().equals(targetId);
		if (!self && !principal.isAdmin()) {
			throw new AccessDeniedException("Only an ADMIN may manage other doctor accounts");
		}
	}

	private void assertEmailAvailable(String email, Long ownDoctorId) {
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new com.keni.doctorappointment.users.EmailAlreadyInUseException(email);
		}
		doctorRepository.findByEmailIgnoreCase(email)
			.filter(existing -> ownDoctorId == null || !existing.getId().equals(ownDoctorId))
			.ifPresent(existing -> {
				throw new com.keni.doctorappointment.users.EmailAlreadyInUseException(email);
			});
	}

}
